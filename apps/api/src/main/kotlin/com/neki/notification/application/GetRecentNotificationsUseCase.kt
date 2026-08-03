package com.neki.notification.application

import com.neki.common.annotation.UseCase
import com.neki.notification.application.dto.NotificationAssembler
import com.neki.notification.application.dto.NotificationResult
import com.neki.notification.dto.NotificationQuery
import com.neki.notification.models.NotificationHist
import com.neki.notification.service.NotificationService
import org.springframework.transaction.annotation.Transactional

/**
 * fileName       : GetRecentNotificationsUseCase
 * author         : darren
 * date           : 2026. 6. 22
 * description    : 사용자의 최근 알림(최신순 최대 30건) 조회
 */
@UseCase
class GetRecentNotificationsUseCase(private val notificationService: NotificationService) {

    @Transactional(readOnly = true)
    fun execute(query: NotificationQuery.GetRecentNotifications): List<NotificationResult.GetRecentNotification> {
        val hists: List<NotificationHist> = notificationService.getRecentHists(query)

        return NotificationAssembler.toRecentNotifications(hists)
    }
}
