package com.neki.notification.application.usecase

import com.neki.common.annotation.UseCase
import com.neki.notification.application.dto.NotificationQuery
import com.neki.notification.application.dto.NotificationResult
import com.neki.notification.application.port.NotificationHistRepositoryPort
import com.neki.notification.entity.NotificationHist
import org.springframework.transaction.annotation.Transactional

/**
 * fileName       : GetRecentNotificationsUseCase
 * author         : darren
 * date           : 2026. 6. 22
 * description    : 사용자의 최근 알림(최신순 최대 30건) 조회
 */
@UseCase
class GetRecentNotificationsUseCase(private val notificationHistRepository: NotificationHistRepositoryPort) {

    @Transactional(readOnly = true)
    fun execute(query: NotificationQuery.GetRecentNotifications): List<NotificationResult.GetRecentNotification> {
        val hists: List<NotificationHist> =
            notificationHistRepository.findRecentByUserId(query.userId)

        return hists.map { hist ->
            NotificationResult.GetRecentNotification(
                id = hist.id!!,
                type = hist.type,
                title = hist.title,
                body = hist.body,
                link = hist.link,
                createdAt = hist.createdAt!!,
            )
        }
    }
}
