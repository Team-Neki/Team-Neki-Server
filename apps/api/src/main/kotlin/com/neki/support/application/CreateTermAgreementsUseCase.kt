package com.neki.support.application

import com.neki.common.annotation.UseCase
import com.neki.support.dto.TermCommand
import com.neki.support.service.TermService
import org.springframework.transaction.annotation.Transactional

@UseCase
class CreateTermAgreementsUseCase(private val termService: TermService) {

    @Transactional
    fun execute(command: TermCommand.CreateTermAgreements) = termService.createAgreements(command)
}
