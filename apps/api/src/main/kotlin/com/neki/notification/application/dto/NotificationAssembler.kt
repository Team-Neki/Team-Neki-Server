package com.neki.notification.application.dto

import com.neki.notification.models.NotificationHist

/**
 * fileName       : NotificationAssembler
 * author         : koo
 * date           : 2026. 8. 4.
 * description    : 알림 이력을 응답 항목으로 조립한다.
 */
object NotificationAssembler {

    fun toRecentNotifications(hists: List<NotificationHist>): List<NotificationResult.GetRecentNotification> =
        hists.map { hist ->
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
