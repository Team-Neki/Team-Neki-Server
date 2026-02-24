package com.neki.term.api.converter

import com.neki.term.api.dto.CreateTermAgreementsRequest
import com.neki.term.application.command.CreateTermAgreementsCommand
import com.neki.term.application.command.TermAgreementItem
import org.springframework.stereotype.Component

@Component
class TermCommandConverter {

    fun toCreateTermAgreementsCommand(userId: Long, request: CreateTermAgreementsRequest): CreateTermAgreementsCommand =
        CreateTermAgreementsCommand(
            userId = userId,
            agreements = request.agreements.map { item ->
                TermAgreementItem(
                    termId = item.termId,
                    agreed = item.agreed,
                )
            },
        )
}
