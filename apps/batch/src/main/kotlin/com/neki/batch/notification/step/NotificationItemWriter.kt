package com.neki.batch.notification.step

import com.neki.core.code.ResultCode
import com.neki.core.exception.BusinessException
import com.neki.domain.notification.dto.NotificationCommand
import com.neki.domain.notification.external.PushNotificationSender
import com.neki.domain.notification.models.FcmSendStatus
import com.neki.domain.notification.models.NotificationLog
import com.neki.domain.notification.repository.NotificationLogRepository
import com.neki.domain.notification.service.NotificationService
import org.slf4j.LoggerFactory
import org.springframework.batch.item.Chunk
import org.springframework.batch.item.ItemWriter
import org.springframework.stereotype.Component

/**
 * fileName       : NotificationItemWriter
 * author         : koo
 * date           : 2026. 10. 6.
 * description    : 1건을 FCM 으로 보내고 결과로 notification_log 를, SUCCESS 면 tb_notification_hist 를 적재한다.
 *                  트랜잭션은 청크 Step(CHUNK_SIZE=1)이 연다. 발송 뒤 적재라 dual-write 이며 그 창은 1건이다.
 *                  PUSH_SEND_FAILED(토큰 무효 등)만 FAILED 로 삼키고 PUSH_NOT_CONFIGURED 는 그대로 올려 잡을 실패시킨다.
 *                  미설정인데 전 건 건너뛰고 COMPLETED 로 끝나는 조용한 실패를 막기 위함이다
 */
@Component
class NotificationItemWriter(
    private val pushNotificationSender: PushNotificationSender,
    private val notificationLogRepository: NotificationLogRepository,
    private val notificationService: NotificationService,
) : ItemWriter<PreparedNotification> {

    private val log = LoggerFactory.getLogger(javaClass)

    override fun write(chunk: Chunk<out PreparedNotification>) {
        chunk.forEach(::dispatch)
    }

    // ponytail: 건별 동기 발송. 대상이 만 단위로 늘어 flow 타임아웃에 걸리면 FirebaseMessaging.sendEach 로 묶는다
    private fun dispatch(prepared: PreparedNotification) {
        val result: FcmSendStatus = try {
            pushNotificationSender.send(prepared.target.fcmToken, prepared.message.title, prepared.message.body, null)
            FcmSendStatus.SUCCESS
        } catch (e: BusinessException) {
            if (e.resultCode != ResultCode.PUSH_SEND_FAILED) throw e
            log.warn("FCM 발송 실패 userId={} type={}", prepared.target.userId, prepared.type)
            FcmSendStatus.FAILED
        }

        notificationLogRepository.save(
            NotificationLog.of(prepared.target, prepared.type, prepared.message, prepared.businessDate, result),
        )
        if (result == FcmSendStatus.SUCCESS) {
            notificationService.recordSentPush(
                NotificationCommand.SendPush(
                    userId = prepared.target.userId,
                    token = prepared.target.fcmToken,
                    type = prepared.type.name,
                    title = prepared.message.title,
                    body = prepared.message.body,
                    link = null,
                ),
            )
        }
    }
}
