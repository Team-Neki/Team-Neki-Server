package com.neki.support.api.converter

import com.neki.support.api.dto.GetTermsResponse
import com.neki.support.api.dto.TermInfoResponse
import com.neki.support.application.dto.TermResult
import org.springframework.stereotype.Component

@Component
class TermResultConverter {

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
