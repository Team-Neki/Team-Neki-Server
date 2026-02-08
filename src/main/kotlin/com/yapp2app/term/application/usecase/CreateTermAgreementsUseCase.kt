package com.yapp2app.term.application.usecase

import com.yapp2app.common.annotation.UseCase
import com.yapp2app.common.api.dto.ResultCode
import com.yapp2app.common.exception.BusinessException
import com.yapp2app.term.application.command.CreateTermAgreementsCommand
import com.yapp2app.term.application.port.TermRepositoryPort
import com.yapp2app.term.application.port.UserTermAgreementRepositoryPort
import com.yapp2app.term.domain.entity.UserTermAgreement
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@UseCase
class CreateTermAgreementsUseCase(
    private val termRepository: TermRepositoryPort,
    private val userTermAgreementRepository: UserTermAgreementRepositoryPort,
) {

    @Transactional
    fun execute(command: CreateTermAgreementsCommand) {
        val agreedTermIds = command.agreements
            .filter { it.agreed }
            .map { it.termId }

        val requiredTerms = termRepository.findAllActiveTerms()
            .filter { it.isRequired }

        val missingRequired = requiredTerms.filter { it.id !in agreedTermIds }
        if (missingRequired.isNotEmpty()) {
            throw BusinessException(ResultCode.REQUIRED_TERMS_NOT_AGREED)
        }

        val termsToAgree = termRepository.findAllByIds(agreedTermIds)
        val now = LocalDateTime.now()

        val agreements = termsToAgree.map { term ->
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
