package com.neki.term.application.usecase

import com.neki.common.annotation.UseCase
import com.neki.term.application.port.TermRepositoryPort
import com.neki.term.application.result.GetTermsResult
import com.neki.term.application.result.TermInfo

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
