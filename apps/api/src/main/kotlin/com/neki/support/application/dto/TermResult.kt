package com.neki.support.application.dto

import com.neki.support.models.TermType

/**
 * fileName       : TermResult
 * author         : koo
 * date           : 2026. 7. 21.
 * description    : Term domain result
 */
object TermResult {
    data class GetTerms(val terms: List<TermInfo>)

    data class TermInfo(
        val id: Long,
        val termType: TermType,
        val title: String,
        val url: String,
        val isRequired: Boolean,
    )

    data class AgreementStatus(val requiredAgreed: Boolean, val marketingAgreed: Boolean)
}
