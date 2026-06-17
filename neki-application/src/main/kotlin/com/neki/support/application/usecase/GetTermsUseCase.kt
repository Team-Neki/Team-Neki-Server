package com.neki.support.application.usecase

import com.neki.common.annotation.UseCase
import com.neki.support.application.port.TermRepositoryPort
import com.neki.support.application.result.GetTermsResult
import com.neki.support.application.result.TermInfo
import com.neki.support.entity.Term

@UseCase
class GetTermsUseCase(private val termRepository: TermRepositoryPort) {

    fun execute(): GetTermsResult {
        val activeTerms: List<Term> = termRepository.findAllActiveTerms()

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
