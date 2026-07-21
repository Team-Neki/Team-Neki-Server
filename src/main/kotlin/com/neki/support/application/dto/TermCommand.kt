package com.neki.support.application.dto

/**
 * fileName       : TermCommand
 * author         : koo
 * date           : 2026. 7. 21.
 * description    : Term domain command
 */
object TermCommand {
    data class CreateTermAgreements(val userId: Long, val agreements: List<TermAgreementItem>)

    data class TermAgreementItem(val termId: Long, val agreed: Boolean)
}
