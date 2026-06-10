package com.neki.support.application.usecase

import com.neki.common.annotation.UseCase
import com.neki.support.application.port.TermRepositoryPort
import com.neki.support.application.port.UserTermAgreementHistRepositoryPort
import com.neki.support.application.port.UserTermAgreementRepositoryPort
import com.neki.support.domain.entity.UserTermAgreementHist
import com.neki.support.domain.enums.TermAgreementAction
import org.springframework.transaction.annotation.Transactional

@UseCase
class RevokeOptionalTermsUseCase(
    private val termRepository: TermRepositoryPort,
    private val userTermAgreementRepository: UserTermAgreementRepositoryPort,
    private val userTermAgreementHistRepository: UserTermAgreementHistRepositoryPort,
) {

    @Transactional
    fun execute(userId: Long) {
        val optionalTermIds: Set<Long> = termRepository.findAllActiveTerms()
            .filter { !it.isRequired }
            .mapNotNull { it.id }
            .toSet()

        val agreedOptionalTermIds: List<Long> = userTermAgreementRepository.findByUserId(userId)
            .map { it.id.termId }
            .filter { it in optionalTermIds }

        if (agreedOptionalTermIds.isEmpty()) return

        userTermAgreementRepository.deleteAllByUserIdAndTermIds(userId, agreedOptionalTermIds)

        val hists: List<UserTermAgreementHist> = agreedOptionalTermIds.map {
            UserTermAgreementHist(userId = userId, termId = it, action = TermAgreementAction.WITHDRAWN)
        }
        userTermAgreementHistRepository.saveAll(hists)
    }
}
