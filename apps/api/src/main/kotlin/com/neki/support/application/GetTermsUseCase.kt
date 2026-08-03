package com.neki.support.application

import com.neki.common.annotation.UseCase
import com.neki.support.application.dto.TermAssembler
import com.neki.support.application.dto.TermResult
import com.neki.support.models.Term
import com.neki.support.service.TermService

@UseCase
class GetTermsUseCase(private val termService: TermService) {

    fun execute(): TermResult.GetTerms {
        val activeTerms: List<Term> = termService.getActiveTerms()

        return TermResult.GetTerms(terms = TermAssembler.toTermInfos(activeTerms))
    }
}
