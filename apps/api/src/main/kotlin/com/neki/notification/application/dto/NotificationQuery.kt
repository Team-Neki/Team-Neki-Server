package com.neki.notification.application.dto

/**
 * fileName       : NotificationQuery
 * author         : koo
 * date           : 2026. 7. 21.
 * description    : Notification domain query
 */
object NotificationQuery {
    data class GetPushAgreement(val userId: Long)

    data class GetRecentNotifications(val userId: Long)
}
