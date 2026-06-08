package com.neki.support.api.converter

import com.neki.support.api.dto.CreateTermAgreementsRequest
import com.neki.support.api.dto.UpdateOptionalTermAgreementRequest
import com.neki.support.application.command.CreateTermAgreementsCommand
import com.neki.support.application.command.TermAgreementItem
import com.neki.support.application.command.UpdateOptionalTermAgreementCommand
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

    fun toUpdateOptionalTermAgreementCommand(
        userId: Long,
        request: UpdateOptionalTermAgreementRequest,
    ): UpdateOptionalTermAgreementCommand = UpdateOptionalTermAgreementCommand(
        userId = userId,
        termId = request.termId,
        agreed = request.agreed,
    )
}
