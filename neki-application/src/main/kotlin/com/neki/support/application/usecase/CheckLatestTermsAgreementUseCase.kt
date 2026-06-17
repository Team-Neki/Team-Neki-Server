package com.neki.support.application.usecase

import com.neki.common.annotation.UseCase
import com.neki.support.application.command.CheckLatestTermsAgreementCommand
import com.neki.support.application.port.TermRepositoryPort
import com.neki.support.application.port.UserTermAgreementRepositoryPort
import com.neki.support.application.result.CheckLatestTermsAgreementResult
import com.neki.support.entity.Term
import com.neki.support.entity.UserTermAgreement

@UseCase
class CheckLatestTermsAgreementUseCase(
    private val termRepository: TermRepositoryPort,
    private val userTermAgreementRepository: UserTermAgreementRepositoryPort,
) {

    fun execute(command: CheckLatestTermsAgreementCommand): CheckLatestTermsAgreementResult {
        val activeTerms: List<Term> = termRepository.findAllActiveTerms()
        val userAgreements: List<UserTermAgreement> = userTermAgreementRepository.findByUserId(command.userId)

        val agreedTermVersions: Set<Pair<Long, String>> = userAgreements.map { it.id.termId to it.termVersion }.toSet()
        val hasAgreedToLatestTerms: Boolean = activeTerms.all { term ->
            (term.id to term.version) in agreedTermVersions
        }

        return CheckLatestTermsAgreementResult(hasAgreedToLatestTerms = hasAgreedToLatestTerms)
    }
}
