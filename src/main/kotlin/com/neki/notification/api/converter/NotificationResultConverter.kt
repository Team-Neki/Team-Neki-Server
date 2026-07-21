package com.neki.notification.api.converter

import com.neki.notification.api.dto.GetRecentNotificationResponse
import com.neki.notification.application.dto.NotificationResult
import org.springframework.stereotype.Component

/**
 * fileName       : NotificationResultConverter
 * author         : darren
 * date           : 2026. 6. 22
 * description    : Notification Result Converter
 */
@Component
class NotificationResultConverter {

    fun toGetRecentNotificationResponse(
        result: List<NotificationResult.GetRecentNotification>,
    ): List<GetRecentNotificationResponse> = result.map {
        GetRecentNotificationResponse(
            id = it.id,
            type = it.type,
            title = it.title,
            body = it.body,
            link = it.link,
            createdAt = it.createdAt,
        )
    }
}
