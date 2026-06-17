package com.neki.support.application.result

import com.neki.support.domain.enums.TermType

data class GetTermsResult(val terms: List<TermInfo>)

data class TermInfo(val id: Long, val termType: TermType, val title: String, val url: String, val isRequired: Boolean)

data class CheckLatestTermsAgreementResult(val hasAgreedToLatestTerms: Boolean)
