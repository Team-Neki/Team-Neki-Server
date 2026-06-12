package com.neki.notification.application.command

/**
 * fileName       : NotificationCommand
 * author         : darren
 * date           : 2026. 6. 12. 16:34
 * description    :
 */
data class GetPushAgreementCommand(val userId: Long)

data class UpdateNotificationCommand(val userId: Long, val deviceToken: String, val pushAgreed: Boolean)
