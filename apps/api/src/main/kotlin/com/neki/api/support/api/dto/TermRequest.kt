package com.neki.api.support.api.dto

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.Valid
import jakarta.validation.constraints.NotEmpty

/**
 * fileName       : TermRequest
 * author         : koo
 * date           : 2026. 7. 21.
 * description    : 약관 관련 요청 DTO
 */
object TermRequest {
    @Schema(name = "CreateTermAgreementsRequest")
    data class CreateTermAgreements(
        @field:Valid
        @field:NotEmpty(message = "약관 동의 항목이 비어있습니다.")
        @field:Schema(description = "약관 동의 항목 목록")
        val agreements: List<TermAgreementItem>,
    )

    @Schema(name = "TermAgreementItemRequest")
    data class TermAgreementItem(
        @field:Schema(description = "약관 ID", example = "1")
        val termId: Long,

        @field:Schema(description = "동의 여부", example = "true")
        val agreed: Boolean,
    )
}
