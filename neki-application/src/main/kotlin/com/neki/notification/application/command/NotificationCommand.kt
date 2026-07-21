package com.neki.notification.application.command

/**
 * fileName       : NotificationCommand
 * author         : darren
 * date           : 2026. 6. 12. 16:34
 * description    :
 */
data class GetPushAgreementCommand(val userId: Long)

data class UpdateNotificationCommand(val userId: Long, val deviceToken: String, val pushAgreed: Boolean)

data class SendPushCommand(
    val userId: Long,
    val token: String,
    val type: String,
    val title: String,
    val body: String,
    val link: String?,
)

data class DeleteNotificationCommand(val userId: Long)

data class GetRecentNotificationsCommand(val userId: Long)
