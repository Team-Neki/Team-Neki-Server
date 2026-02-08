package com.yapp2app.term.application.command

data class CreateTermAgreementsCommand(val userId: Long, val agreements: List<TermAgreementItem>)

data class TermAgreementItem(val termId: Long, val agreed: Boolean)

data class CheckLatestTermsAgreementCommand(val userId: Long)
