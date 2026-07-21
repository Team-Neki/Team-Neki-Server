package com.neki.notification.api.converter

import com.neki.notification.api.dto.UpdateNotificationRequest
import com.neki.notification.application.command.UpdateNotificationCommand
import org.springframework.stereotype.Component

/**
 * fileName       : NotificationCommandConverter
 * author         : darren
 * date           : 2026. 6. 12
 * description    :
 */
@Component
class NotificationCommandConverter {

    fun toUpdateNotificationCommand(userId: Long, request: UpdateNotificationRequest): UpdateNotificationCommand =
        UpdateNotificationCommand(
            userId = userId,
            deviceToken = request.deviceToken,
            pushAgreed = request.pushAgreed,
        )
}
