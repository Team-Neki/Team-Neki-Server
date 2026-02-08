package com.yapp2app.term.api.converter

import com.yapp2app.term.api.dto.GetTermsResponse
import com.yapp2app.term.api.dto.TermInfoResponse
import com.yapp2app.term.application.result.GetTermsResult
import org.springframework.stereotype.Component

@Component
class TermResultConverter {

    fun toGetTermsResponse(result: GetTermsResult): GetTermsResponse = GetTermsResponse(
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
