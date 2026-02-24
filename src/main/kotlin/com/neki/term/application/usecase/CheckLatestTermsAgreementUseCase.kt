package com.neki.term.application.usecase

import com.neki.common.annotation.UseCase
import com.neki.term.application.command.CheckLatestTermsAgreementCommand
import com.neki.term.application.port.TermRepositoryPort
import com.neki.term.application.port.UserTermAgreementRepositoryPort
import com.neki.term.application.result.CheckLatestTermsAgreementResult

@UseCase
class CheckLatestTermsAgreementUseCase(
    private val termRepository: TermRepositoryPort,
    private val userTermAgreementRepository: UserTermAgreementRepositoryPort,
) {

    fun execute(command: CheckLatestTermsAgreementCommand): CheckLatestTermsAgreementResult {
        val activeTerms = termRepository.findAllActiveTerms()
        val userAgreements = userTermAgreementRepository.findByUserId(command.userId)

        val agreedTermVersions = userAgreements.map { it.id.termId to it.termVersion }.toSet()
        val hasAgreedToLatestTerms = activeTerms.all { term ->
            (term.id to term.version) in agreedTermVersions
        }

        return CheckLatestTermsAgreementResult(hasAgreedToLatestTerms = hasAgreedToLatestTerms)
    }
}
