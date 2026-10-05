package com.neki.batch.notification.step

import com.neki.domain.notification.NotificationProcessor
import com.neki.domain.notification.models.NotificationType
import com.neki.domain.notification.models.SendDecision
import com.neki.domain.notification.models.SendTarget
import com.neki.domain.notification.repository.NotificationLogRepository
import org.springframework.batch.item.ItemProcessor
import java.time.LocalDate

/** 당일 중복을 조회해 도메인 판정에 넘긴다. Skip 이면 null 을 돌려 청크에서 필터된다 */
class NotificationItemProcessor(
    private val type: NotificationType,
    private val businessDate: LocalDate,
    private val notificationLogRepository: NotificationLogRepository,
) : ItemProcessor<SendTarget, PreparedNotification> {

    override fun process(item: SendTarget): PreparedNotification? {
        val alreadySent: Boolean = notificationLogRepository.exists(item.userId, type, businessDate)
        return when (val decision = NotificationProcessor.decide(item, type, alreadySent, businessDate)) {
            is SendDecision.Send -> PreparedNotification(item, type, decision.message, businessDate)
            is SendDecision.Skip -> null
        }
    }
}
