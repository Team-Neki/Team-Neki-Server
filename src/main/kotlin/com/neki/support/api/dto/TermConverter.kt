package com.neki.support.api.dto

import com.neki.support.application.dto.TermCommand
import com.neki.support.application.dto.TermResult
import org.springframework.stereotype.Component

/**
 * fileName       : TermConverter
 * author         : koo
 * date           : 2026. 7. 21.
 * description    : Term api layer converter
 */
object TermConverter {
    @Component
    class RequestConverter {
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

    @Component
    class ResponseConverter {
        fun toGetTermsResponse(result: TermResult.GetTerms): GetTermsResponse = GetTermsResponse(
            terms = result.terms.map { termInfo ->
                TermInfoResponse(
                    id = termInfo.id,
                    termType = termInfo.termType.name,
                    title = termInfo.title,
                    url = termInfo.url,
                    isRequired = termInfo.isRequired,
                )
            },
        )
    }
}
