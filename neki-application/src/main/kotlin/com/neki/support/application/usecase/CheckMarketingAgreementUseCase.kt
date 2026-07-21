package com.neki.support.application.usecase

import com.neki.common.annotation.UseCase
import com.neki.support.application.port.TermRepositoryPort
import com.neki.support.application.port.UserTermAgreementRepositoryPort
import com.neki.support.application.result.TermAgreementResult
import com.neki.support.enums.TermType

@UseCase
class CheckMarketingAgreementUseCase(
    private val termRepository: TermRepositoryPort,
    private val userTermAgreementRepository: UserTermAgreementRepositoryPort,
) {

    fun execute(userId: Long): TermAgreementResult {
        val marketingTerm = termRepository.findActiveByTermType(TermType.MARKETING)
            ?: return TermAgreementResult(agreed = false)
        val agreement = userTermAgreementRepository.findByUserIdAndTermId(userId, marketingTerm.id!!)
        return TermAgreementResult(agreed = agreement != null)
    }
}
