package com.neki.api.share.api.dto

import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime

/**
 * fileName       : ShareLinkResponse
 * author         : darren
 * date           : 2026. 11. 26.
 * description    : 공유 링크 관련 응답 DTO
 */
object ShareLinkResponse {
    @Schema(name = "IssueShareLinkResponse")
    data class IssueShareLink(
        @field:Schema(description = "공유 토큰 (URL-safe 43자)", example = "q3Zb9Yx0vP2mK7tR1sW8nL4cF6hJ5dG0aE2uI9oB3xQ")
        val token: String,
        @field:Schema(
            description = "공유 웹페이지 URL (카톡 전송용, staging 은 ?env=dev). 이미지 바이너리는 GET /share/{token}",
            example = "https://neki-share.suitestudy.com/s/q3Zb9Yx0vP2mK7tR1sW8nL4cF6hJ5dG0aE2uI9oB3xQ",
        )
        val url: String,
        @field:Schema(description = "만료 일시 (KST)", example = "2026-12-03T12:00:00")
        val expiresAt: LocalDateTime,
    )
}
