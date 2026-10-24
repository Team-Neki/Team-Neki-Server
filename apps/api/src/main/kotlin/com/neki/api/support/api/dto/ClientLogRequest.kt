package com.neki.api.support.api.dto

import com.neki.domain.support.models.Platform
import io.swagger.v3.oas.annotations.media.ArraySchema
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Positive
import jakarta.validation.constraints.Size
import java.time.Instant
import java.util.UUID

/**
 * fileName       : ClientLogRequest
 * author         : koo
 * date           : 2026. 10. 2.
 * description    : 클라이언트 로그 수집 요청 DTO
 */
object ClientLogRequest {
    @Schema(name = "CollectClientLogsRequest")
    data class Collect(
        @field:Schema(description = "앱 플랫폼", example = "IOS")
        val platform: Platform,

        @field:Size(min = 1, max = 500, message = "로그는 1건 이상 500건 이하로 보내야 합니다.")
        @field:Valid
        @field:ArraySchema(arraySchema = Schema(description = "발생 시점 정보를 포함한 행동 이벤트 목록"))
        val logs: List<Event>,
    )

    data class Event(
        @field:Schema(description = "이벤트가 생길 때 만든 UUID. 재전송 시 유지")
        val eventId: UUID,
        @field:NotBlank
        @field:Size(max = 64)
        val eventType: String,
        val eventProperties: Map<String, Any?>,
        @field:Schema(description = "이벤트 발생 시각. UTC ISO 8601")
        val occurredAt: Instant,
        @field:Positive
        @field:Schema(description = "분석 세션 시작 시각. epoch milliseconds")
        val sessionId: Long,
        @field:Positive
        @field:Schema(description = "같은 기기·세션에서 이벤트가 생길 때 증가시키는 순번")
        val sequenceNumber: Int,
        @field:NotBlank
        val deviceId: String,
        @field:Schema(description = "이벤트 발생 당시 앱이 알고 있던 사용자 ID. 익명은 null. 인증된 ID와 별도로 보관")
        val userIdAtOccurrence: String? = null,
        @field:NotBlank
        val appVersion: String,
        @field:Schema(description = "발생 당시의 앱 빌드·기기·OS 등 SDK 자동 속성")
        val context: Map<String, Any?>,
    )
}
