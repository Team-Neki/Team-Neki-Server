package com.neki.api.support.api.dto

import com.neki.domain.support.models.Platform
import io.swagger.v3.oas.annotations.media.ArraySchema
import io.swagger.v3.oas.annotations.media.Schema
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

        // 요청 하나의 건수 상한. 바이트 상한(로그 1,000KiB, 요청 4MiB)은 Firehose 어댑터가 본다
        @field:Size(min = 1, max = 500, message = "로그는 1건 이상 500건 이하로 보내야 합니다.")
        @field:ArraySchema(
            arraySchema = Schema(
                description = "로그 목록. 각 원소는 형식 제한이 없는 JSON 객체. " +
                    "appVersion 은 로그가 발생한 시점의 앱 버전을 각 로그에 넣는다. " +
                    "eventId 는 로그가 생길 때 한 번 만든 UUID 로, 재전송해도 바꾸지 않는다",
            ),
            schema = Schema(
                type = "object",
                example = """{"eventId":"0f8b6c2e-3d4a-4f5b-9c1d-2e7a8b9c0d1e","appVersion":"1.4.0",""" +
                    """"level":"ERROR","message":"photo upload failed",""" +
                    """"occurredAt":"2026-10-02T10:00:00Z"}""",
            ),
        )
        val logs: List<Map<String, Any?>>,
    )
}
