package com.neki.domain.notification

import com.neki.domain.notification.models.NotificationType
import com.neki.domain.notification.models.RenderedMessage
import com.neki.domain.notification.models.SendDecision
import com.neki.domain.notification.models.SendTarget
import com.neki.domain.notification.models.SkipReason
import java.time.LocalDate

/**
 * fileName       : NotificationProcessor
 * author         : koo
 * date           : 2026. 10. 6.
 * description    : 대상 1건의 판정. 중복이면 Skip, 아니면 톤 배정 + 렌더. 동의는 읽기 쿼리가 보므로 재확인하지 않는다.
 *                  이력 조회는 호출자가 하고 그 결과(alreadySent)만 받는다
 */
object NotificationProcessor {

    fun decide(
        target: SendTarget,
        type: NotificationType,
        alreadySent: Boolean,
        businessDate: LocalDate,
    ): SendDecision {
        if (alreadySent) {
            return SendDecision.Skip(SkipReason.ALREADY_SENT)
        }
        val message: RenderedMessage =
            MessageRenderer.render(type, ToneAssignmentPolicy.assign(target.userId, businessDate), target.variables)
        return SendDecision.Send(message)
    }
}
