package com.yapp2app.term.api.converter

import com.yapp2app.term.api.dto.CreateTermAgreementsRequest
import com.yapp2app.term.application.command.CreateTermAgreementsCommand
import com.yapp2app.term.application.command.TermAgreementItem
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
