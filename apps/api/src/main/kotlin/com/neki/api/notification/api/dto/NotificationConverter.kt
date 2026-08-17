package com.neki.api.notification.api.dto

import com.neki.api.notification.application.dto.NotificationResult
import com.neki.domain.notification.dto.NotificationCommand
import org.springframework.stereotype.Component

/**
 * fileName       : NotificationConverter
 * author         : koo
 * date           : 2026. 7. 21.
 * description    : Notification api layer converter
 */
object NotificationConverter {
    @Component
    class RequestConverter {
        fun toUpdateNotificationCommand(
            userId: Long,
            request: NotificationRequest.UpdateNotification,
        ): NotificationCommand.UpdateNotification = NotificationCommand.UpdateNotification(
            userId = userId,
            deviceToken = request.deviceToken,
            pushAgreed = request.pushAgreed,
        )
    }

    @Component
    class ResponseConverter {
        fun toGetRecentNotificationResponse(
            result: List<NotificationResult.GetRecentNotification>,
        ): List<NotificationResponse.GetRecentNotification> = result.map {
            NotificationResponse.GetRecentNotification(
                id = it.id,
                type = it.type,
                title = it.title,
                body = it.body,
                link = it.link,
                createdAt = it.createdAt,
            )
        }
    }
}
