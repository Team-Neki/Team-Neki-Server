package com.neki.domain.notification.dto

/**
 * fileName       : NotificationCommand
 * author         : koo
 * date           : 2026. 7. 21.
 * description    : Notification domain command
 */
object NotificationCommand {
    data class UpdateNotification(val userId: Long, val deviceToken: String, val pushAgreed: Boolean)

    data class SendPush(
        val userId: Long,
        val token: String,
        val type: String,
        val title: String,
        val body: String,
        val link: String?,
    )

    data class DeleteNotification(val userId: Long)
}
