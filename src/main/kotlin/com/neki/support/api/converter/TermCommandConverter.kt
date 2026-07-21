package com.neki.support.api.converter

import com.neki.support.api.dto.CreateTermAgreementsRequest
import com.neki.support.application.dto.TermCommand
import org.springframework.stereotype.Component

@Component
class TermCommandConverter {

    fun toCreateTermAgreementsCommand(
        userId: Long,
        request: CreateTermAgreementsRequest,
    ): TermCommand.CreateTermAgreements = TermCommand.CreateTermAgreements(
        userId = userId,
        agreements = request.agreements.map { item ->
            TermCommand.TermAgreementItem(
                termId = item.termId,
                agreed = item.agreed,
            )
        },
    )
}
