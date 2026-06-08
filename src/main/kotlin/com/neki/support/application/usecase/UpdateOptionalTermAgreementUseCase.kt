package com.neki.support.application.usecase

import com.neki.common.annotation.UseCase
import com.neki.common.api.dto.ResultCode
import com.neki.common.exception.BusinessException
import com.neki.support.application.command.UpdateOptionalTermAgreementCommand
import com.neki.support.application.port.TermRepositoryPort
import com.neki.support.application.port.UserTermAgreementRepositoryPort
import com.neki.support.domain.entity.UserTermAgreement
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@UseCase
class UpdateOptionalTermAgreementUseCase(
    private val termRepository: TermRepositoryPort,
    private val userTermAgreementRepository: UserTermAgreementRepositoryPort,
) {

    @Transactional
    fun execute(command: UpdateOptionalTermAgreementCommand) {
        val term = termRepository.findById(command.termId)
            ?: throw BusinessException(ResultCode.NOT_FOUND)

        if (term.isRequired) {
            throw BusinessException(ResultCode.CANNOT_UPDATE_REQUIRED_TERM)
        }

        val existing = userTermAgreementRepository.findByUserIdAndTermId(command.userId, command.termId)
        val now = LocalDateTime.now()

        if (command.agreed) {
            if (existing != null) {
                existing.agreedAt = now
                existing.termVersion = term.version
                existing.withdrawnAt = null
                userTermAgreementRepository.save(existing)
            } else {
                userTermAgreementRepository.save(
                    UserTermAgreement(
                        userId = command.userId,
                        termId = term.id!!,
                        agreedAt = now,
                        termVersion = term.version,
                    ),
                )
            }
        } else {
            if (existing != null && existing.withdrawnAt == null) {
                existing.withdrawnAt = now
                userTermAgreementRepository.save(existing)
            }
        }
    }
}
