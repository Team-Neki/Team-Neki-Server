package com.neki.notification.api.converter

import com.neki.notification.api.dto.UpdateNotificationRequest
import com.neki.notification.application.dto.NotificationCommand
import org.springframework.stereotype.Component

/**
 * fileName       : NotificationCommandConverter
 * author         : darren
 * date           : 2026. 6. 12
 * description    :
 */
@Component
class NotificationCommandConverter {

    fun toUpdateNotificationCommand(
        userId: Long,
        request: UpdateNotificationRequest,
    ): NotificationCommand.UpdateNotification = NotificationCommand.UpdateNotification(
        userId = userId,
        deviceToken = request.deviceToken,
        pushAgreed = request.pushAgreed,
    )
}
