package com.neki.notification.application.result

import java.time.LocalDateTime

/**
 * fileName       : GetRecentNotificationResult
 * author         : darren
 * date           : 2026. 6. 22
 * description    : 최근 알림 조회 result
 */
data class GetRecentNotificationResult(
    val id: Long,
    val type: String,
    val title: String,
    val body: String,
    val link: String?,
    val createdAt: LocalDateTime,
)
