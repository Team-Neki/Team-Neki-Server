package com.neki.batch.notification.tasklet

import com.neki.core.code.ResultCode
import com.neki.core.exception.BusinessException
import com.neki.domain.notification.NotificationProcessor
import com.neki.domain.notification.dto.NotificationCommand
import com.neki.domain.notification.external.PushNotificationSender
import com.neki.domain.notification.models.NotificationLog
import com.neki.domain.notification.models.NotificationType
import com.neki.domain.notification.models.PushSendStatus
import com.neki.domain.notification.models.RenderedMessage
import com.neki.domain.notification.models.SendDecision
import com.neki.domain.notification.models.SendTarget
import com.neki.domain.notification.repository.NotificationLogRepository
import com.neki.domain.notification.service.NotificationService
import org.slf4j.LoggerFactory
import org.springframework.batch.core.StepContribution
import org.springframework.batch.core.scope.context.ChunkContext
import org.springframework.batch.core.step.tasklet.Tasklet
import org.springframework.batch.repeat.RepeatStatus
import java.time.LocalDate

/**
 * fileName       : PushNotificationTasklet
 * author         : koo
 * date           : 2026. 10. 6.
 * description    : 알림 발송 잡 3종이 공유하는 tasklet. execute() 1회가 대상 1건이다.
 *                  동의자 페이지를 버퍼에 당겨 1건씩 중복 판정 -> 톤·문구 -> 발송 -> notification_log -> SUCCESS 면 tb_notification_hist.
 *                  TaskletStep 이 execute() 마다 트랜잭션을 열고 닫으므로 커밋 단위 = 1건이다. 발송은 트랜잭션 밖 부수효과라
 *                  적재가 실패하면 그 1건만 재실행 때 중복될 수 있다 (at-least-once, pipeline.md 1절).
 *                  PUSH_SEND_FAILED(토큰 무효 등)만 FAILED 로 적재하고 계속, PUSH_NOT_CONFIGURED 등은 그대로 올려 step 을 실패시킨다.
 *                  미설정인데 전 건 건너뛰고 COMPLETED 로 끝나는 조용한 실패를 막기 위함이다.
 *                  버퍼·커서 상태를 가지므로 @StepScope 빈으로만 쓴다 (실행마다 새 인스턴스). 단일 쓰레드 전제
 */
class PushNotificationTasklet(
    private val type: NotificationType,
    private val businessDate: LocalDate,
    private val reader: SendTargetReader,
    private val pageSize: Int,
    private val notificationLogRepository: NotificationLogRepository,
    private val pushNotificationSender: PushNotificationSender,
    private val notificationService: NotificationService,
) : Tasklet {

    private val log = LoggerFactory.getLogger(javaClass)

    private val buffer = ArrayDeque<SendTarget>()
    private var cursor = 0L
    private var exhausted = false

    override fun execute(contribution: StepContribution, chunkContext: ChunkContext): RepeatStatus {
        val target: SendTarget = next() ?: return RepeatStatus.FINISHED
        contribution.incrementReadCount()

        val alreadySent: Boolean = notificationLogRepository.exists(target.userId, type, businessDate)
        when (val decision = NotificationProcessor.decide(target, type, alreadySent, businessDate)) {
            is SendDecision.Skip -> contribution.incrementFilterCount(1)
            is SendDecision.Send -> {
                dispatch(target, decision.message)
                contribution.incrementWriteCount(1)
            }
        }
        return RepeatStatus.CONTINUABLE
    }

    /** 버퍼가 비고 소진이 아닌 동안 다음 페이지를 당긴다. targets 가 비어도 nextCursor 가 있으면 소진이 아니다 (SendTargetPage) */
    private fun next(): SendTarget? {
        while (buffer.isEmpty() && !exhausted) {
            val page: SendTargetPage = reader.readPage(cursor, pageSize)
            buffer.addAll(page.targets)
            val nextCursor: Long? = page.nextCursor
            if (nextCursor == null) exhausted = true else cursor = nextCursor
        }
        return buffer.removeFirstOrNull()
    }

    // ponytail: 건별 동기 발송. 대상이 만 단위로 늘어 flow 타임아웃에 걸리면 포트에 일괄 발송을 더한다 (어댑터는 FCM sendEach)
    private fun dispatch(target: SendTarget, message: RenderedMessage) {
        val status: PushSendStatus = try {
            pushNotificationSender.send(target.deviceToken, message.title, message.body, null)
            PushSendStatus.SUCCESS
        } catch (e: BusinessException) {
            if (e.resultCode != ResultCode.PUSH_SEND_FAILED) throw e
            log.warn("푸시 발송 실패 userId={} type={}", target.userId, type)
            PushSendStatus.FAILED
        }

        notificationLogRepository.save(NotificationLog.of(target, type, message, businessDate, status))
        if (status == PushSendStatus.SUCCESS) {
            notificationService.recordSentPush(
                NotificationCommand.SendPush(
                    userId = target.userId,
                    token = target.deviceToken,
                    type = type.name,
                    title = message.title,
                    body = message.body,
                    link = null,
                ),
            )
        }
    }

    companion object {
        /** Prefect 가 넘기는 잡 파라미터. 발송의 논리적 날짜 (businessDate=2026-10-06) */
        const val PARAM_BUSINESS_DATE = "businessDate"
    }
}
