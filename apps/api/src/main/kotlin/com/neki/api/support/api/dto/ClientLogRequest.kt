package com.neki.api.support.api.dto

import com.neki.domain.support.models.Platform
import io.swagger.v3.oas.annotations.media.ArraySchema
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

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

        @field:NotBlank(message = "앱 버전은 필수입니다.")
        @field:Size(max = 32, message = "앱 버전은 32자 이하여야 합니다.")
        @field:Schema(description = "앱 버전", example = "1.4.0")
        val appVersion: String,

        // 500 은 Firehose PutRecordBatch 한 번에 넣을 수 있는 최대 건수
        @field:Size(min = 1, max = 500, message = "로그는 1건 이상 500건 이하로 보내야 합니다.")
        @field:ArraySchema(
            arraySchema = Schema(description = "로그 목록. 각 원소는 형식 제한이 없는 JSON 객체"),
            schema = Schema(
                type = "object",
                example = """{"level":"ERROR","message":"photo upload failed","occurredAt":"2026-10-02T10:00:00Z"}""",
            ),
        )
        val logs: List<Map<String, Any?>>,
    )
}
