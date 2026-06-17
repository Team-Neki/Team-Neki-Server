package com.neki.support.api.dto

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.Valid
import jakarta.validation.constraints.NotEmpty

data class CreateTermAgreementsRequest(
    @field:Valid
    @field:NotEmpty(message = "약관 동의 항목이 비어있습니다.")
    @field:Schema(description = "약관 동의 항목 목록")
    val agreements: List<TermAgreementItemRequest>,
)

data class TermAgreementItemRequest(
    @field:Schema(description = "약관 ID", example = "1")
    val termId: Long,

    @field:Schema(description = "동의 여부", example = "true")
    val agreed: Boolean,
)
