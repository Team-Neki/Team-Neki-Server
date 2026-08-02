package com.neki.support.application.usecase

import com.neki.common.annotation.UseCase
import com.neki.support.application.dto.TermResult
import com.neki.support.application.port.TermRepositoryPort
import com.neki.support.entity.Term

@UseCase
class GetTermsUseCase(private val termRepository: TermRepositoryPort) {

    fun execute(): TermResult.GetTerms {
        val activeTerms: List<Term> = termRepository.findAllActiveTerms()

        return TermResult.GetTerms(
            terms = activeTerms
                .map { term ->
                    TermResult.TermInfo(
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
