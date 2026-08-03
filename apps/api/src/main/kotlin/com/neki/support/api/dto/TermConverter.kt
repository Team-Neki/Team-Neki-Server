package com.neki.support.api.dto

import com.neki.support.application.dto.TermResult
import com.neki.support.dto.TermCommand
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
            request: TermRequest.CreateTermAgreements,
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
        fun toGetTermsResponse(result: TermResult.GetTerms): TermResponse.GetTerms = TermResponse.GetTerms(
            terms = result.terms.map { termInfo ->
                TermResponse.TermInfo(
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
