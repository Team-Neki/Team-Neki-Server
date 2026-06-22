package com.neki.notification.api.converter

import com.neki.notification.api.dto.GetRecentNotificationResponse
import com.neki.notification.application.result.GetRecentNotificationResult
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
        result: List<GetRecentNotificationResult>,
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
