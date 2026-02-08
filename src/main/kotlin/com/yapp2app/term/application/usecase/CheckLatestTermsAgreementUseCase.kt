package com.yapp2app.term.application.usecase

import com.yapp2app.common.annotation.UseCase
import com.yapp2app.term.application.command.CheckLatestTermsAgreementCommand
import com.yapp2app.term.application.port.TermRepositoryPort
import com.yapp2app.term.application.port.UserTermAgreementRepositoryPort
import com.yapp2app.term.application.result.CheckLatestTermsAgreementResult

@UseCase
class CheckLatestTermsAgreementUseCase(
    private val termRepository: TermRepositoryPort,
    private val userTermAgreementRepository: UserTermAgreementRepositoryPort,
) {

    fun execute(command: CheckLatestTermsAgreementCommand): CheckLatestTermsAgreementResult {
        val activeTerms = termRepository.findAllActiveTerms()
        val userAgreements = userTermAgreementRepository.findByUserId(command.userId)

        val hasAgreedToLatestTerms = activeTerms.all { term ->
            userAgreements.any { agreement ->
                agreement.id.termId == term.id && agreement.termVersion == term.version
            }
        }

        return CheckLatestTermsAgreementResult(hasAgreedToLatestTerms = hasAgreedToLatestTerms)
    }
}
