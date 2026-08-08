package com.neki.api.notification.application.dto

import java.time.LocalDateTime

/**
 * fileName       : NotificationResult
 * author         : koo
 * date           : 2026. 7. 21.
 * description    : Notification domain result
 */
object NotificationResult {
    data class GetPushAgreement(val pushAgreed: Boolean)

    data class GetRecentNotification(
        val id: Long,
        val type: String,
        val title: String,
        val body: String,
        val link: String?,
        val createdAt: LocalDateTime,
    )

    data class SendPush(val messageId: String)
}
