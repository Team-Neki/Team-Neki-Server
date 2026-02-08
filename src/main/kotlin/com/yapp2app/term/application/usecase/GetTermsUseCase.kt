package com.yapp2app.term.application.usecase

import com.yapp2app.common.annotation.UseCase
import com.yapp2app.term.application.port.TermRepositoryPort
import com.yapp2app.term.application.result.GetTermsResult
import com.yapp2app.term.application.result.TermInfo

@UseCase
class GetTermsUseCase(private val termRepository: TermRepositoryPort) {

    fun execute(): GetTermsResult {
        val activeTerms = termRepository.findAllActiveTerms()

        return GetTermsResult(
            terms = activeTerms
                .map { term ->
                    TermInfo(
                        id = term.id!!,
                        termType = term.termType,
                        title = term.title,
                        url = term.url,
                        isRequired = term.isRequired,
                    )
                },
        )
    }
}
