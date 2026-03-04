package com.neki.support.application.usecase

import com.neki.common.annotation.UseCase
import com.neki.common.api.dto.ResultCode
import com.neki.common.exception.BusinessException
import com.neki.support.application.command.CreateTermAgreementsCommand
import com.neki.support.application.port.TermRepositoryPort
import com.neki.support.application.port.UserTermAgreementRepositoryPort
import com.neki.support.domain.entity.Term
import com.neki.support.domain.entity.UserTermAgreement
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@UseCase
class CreateTermAgreementsUseCase(
    private val termRepository: TermRepositoryPort,
    private val userTermAgreementRepository: UserTermAgreementRepositoryPort,
) {

    @Transactional
    fun execute(command: CreateTermAgreementsCommand) {
        val agreedTermIds: List<Long> = command.agreements
            .filter { it.agreed }
            .map { it.termId }

        val requiredTerms: List<Term> = termRepository.findAllActiveTerms()
            .filter { it.isRequired }

        val missingRequired: List<Term> = requiredTerms.filter { it.id !in agreedTermIds }
        if (missingRequired.isNotEmpty()) {
            throw BusinessException(ResultCode.REQUIRED_TERMS_NOT_AGREED)
        }

        val termsToAgree: List<Term> = termRepository.findAllByIds(agreedTermIds)
        val now = LocalDateTime.now()

        val agreements: List<UserTermAgreement> = termsToAgree.map { term ->
            UserTermAgreement(
                userId = command.userId,
                termId = term.id!!,
                agreedAt = now,
                termVersion = term.version,
            )
        }

        userTermAgreementRepository.saveAll(agreements)
    }
}
