package com.neki.api.support.api.dto

import io.swagger.v3.oas.annotations.media.Schema

/**
 * fileName       : TermResponse
 * author         : koo
 * date           : 2026. 7. 21.
 * description    : 약관 관련 응답 DTO
 */
object TermResponse {
    @Schema(name = "GetTermsResponse")
    data class GetTerms(
        @field:Schema(description = "약관 목록")
        val terms: List<TermInfo>,
    )

    @Schema(name = "TermInfoResponse")
    data class TermInfo(
        @field:Schema(description = "약관 ID", example = "1")
        val id: Long,

        @field:Schema(description = "약관 종류", example = "SERVICE")
        val termType: String,

        @field:Schema(description = "약관 제목", example = "서비스 이용약관")
        val title: String,

        @field:Schema(description = "약관 URL", example = "https://example.com/terms/service")
        val url: String,

        @field:Schema(description = "필수 동의 여부", example = "true")
        val isRequired: Boolean,
    )
}
