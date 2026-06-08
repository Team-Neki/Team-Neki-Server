package com.neki.support.application.usecase

import com.neki.common.annotation.UseCase
import com.neki.support.application.port.TermRepositoryPort
import com.neki.support.application.port.UserTermAgreementRepositoryPort
import com.neki.support.domain.enums.TermType

@UseCase
class CheckMarketingAgreementUseCase(
    private val termRepository: TermRepositoryPort,
    private val userTermAgreementRepository: UserTermAgreementRepositoryPort,
) {

    fun execute(userId: Long): Boolean {
        val marketingTerm = termRepository.findActiveByTermType(TermType.MARKETING) ?: return false
        val agreement = userTermAgreementRepository.findByUserIdAndTermId(userId, marketingTerm.id!!)
        return agreement != null && agreement.withdrawnAt == null
    }
}
