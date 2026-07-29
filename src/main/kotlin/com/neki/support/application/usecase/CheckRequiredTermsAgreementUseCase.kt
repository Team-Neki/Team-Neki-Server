package com.neki.support.application.usecase

import com.neki.common.annotation.UseCase
import com.neki.support.application.dto.TermQuery
import com.neki.support.application.dto.TermResult
import com.neki.support.application.port.TermRepositoryPort
import com.neki.support.application.port.UserTermAgreementRepositoryPort
import com.neki.support.domain.entity.Term
import com.neki.support.domain.entity.UserTermAgreement

@UseCase
class CheckRequiredTermsAgreementUseCase(
    private val termRepository: TermRepositoryPort,
    private val userTermAgreementRepository: UserTermAgreementRepositoryPort,
) {

    fun execute(query: TermQuery.CheckRequiredTermsAgreement): TermResult.TermAgreement {
        val activeTerms: List<Term> = termRepository.findAllActiveRequiredTerms()
        val userAgreements: List<UserTermAgreement> = userTermAgreementRepository.findByUserId(query.userId)

        val agreedTermVersions: Set<Pair<Long, String>> = userAgreements.map { it.id.termId to it.termVersion }.toSet()
        val hasAgreedToAllRequired: Boolean = activeTerms.all { term ->
            (term.id to term.version) in agreedTermVersions
        }

        return TermResult.TermAgreement(agreed = hasAgreedToAllRequired)
    }
}
