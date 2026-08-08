package com.neki.api.support.application

import com.neki.core.annotation.UseCase
import com.neki.domain.support.dto.AppVersionCommand
import com.neki.domain.support.service.AppVersionService
import org.springframework.transaction.annotation.Transactional

/**
 * fileName       : UpdateAppVersionUseCase
 * author         : darren
 * date           : 2026. 1. 29
 * description    : version 변경
 */
@UseCase
class UpdateAppVersionUseCase(private val appVersionService: AppVersionService) {

    @Transactional
    fun execute(command: AppVersionCommand.UpdateAppVersion) {
        appVersionService.updateVersion(command)
    }
}
