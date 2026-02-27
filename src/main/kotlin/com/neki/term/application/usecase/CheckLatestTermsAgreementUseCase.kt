package com.neki.term.application.usecase

import com.neki.common.annotation.UseCase
import com.neki.term.application.command.CheckLatestTermsAgreementCommand
import com.neki.term.application.port.TermRepositoryPort
import com.neki.term.application.port.UserTermAgreementRepositoryPort
import com.neki.term.application.result.CheckLatestTermsAgreementResult
import com.neki.term.domain.entity.Term
import com.neki.term.domain.entity.UserTermAgreement

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
