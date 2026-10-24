package com.neki.domain.support.dto

import com.neki.domain.support.models.Platform
import java.time.Instant
import java.util.UUID

/**
 * fileName       : ClientLogCommand
 * author         : koo
 * date           : 2026. 10. 2.
 * description    : 클라이언트 로그 수집 command
 */
object ClientLogCommand {
    data class Collect(val userId: Long?, val platform: Platform, val logs: List<Event>)

    data class Event(
        val eventId: UUID,
        val eventType: String,
        val eventProperties: Map<String, Any?>,
        val occurredAt: Instant,
        val sessionId: Long,
        val sequenceNumber: Int,
        val deviceId: String,
        val userIdAtOccurrence: String?,
        val appVersion: String,
        val context: Map<String, Any?>,
    )
}
