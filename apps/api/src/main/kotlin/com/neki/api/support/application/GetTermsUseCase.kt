package com.neki.api.support.application

import com.neki.api.support.application.dto.TermAssembler
import com.neki.api.support.application.dto.TermResult
import com.neki.core.annotation.UseCase
import com.neki.domain.support.models.Term
import com.neki.domain.support.service.TermService

@UseCase
class GetTermsUseCase(private val termService: TermService) {

    fun execute(): TermResult.GetTerms {
        val activeTerms: List<Term> = termService.getActiveTerms()

        return TermResult.GetTerms(terms = TermAssembler.toTermInfos(activeTerms))
    }
}
