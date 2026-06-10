package com.neki.support.api.converter

import com.neki.support.api.dto.CreateTermAgreementsRequest
import com.neki.support.application.command.CreateTermAgreementsCommand
import com.neki.support.application.command.TermAgreementItem
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
