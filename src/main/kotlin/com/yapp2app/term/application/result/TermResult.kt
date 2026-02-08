package com.yapp2app.term.application.result

import com.yapp2app.term.domain.enums.TermType

data class GetTermsResult(val terms: List<TermInfo>)

data class TermInfo(val id: Long, val termType: TermType, val title: String, val url: String, val isRequired: Boolean)

data class CheckLatestTermsAgreementResult(val hasAgreedToLatestTerms: Boolean)
