package com.neki.api.support.application

import com.neki.core.annotation.UseCase
import com.neki.domain.support.dto.TermCommand
import com.neki.domain.support.service.TermService
import org.springframework.transaction.annotation.Transactional

@UseCase
class RevokeOptionalTermsUseCase(private val termService: TermService) {

    @Transactional
    fun execute(command: TermCommand.RevokeOptionalTerms) = termService.revokeOptionalTerms(command)
}
