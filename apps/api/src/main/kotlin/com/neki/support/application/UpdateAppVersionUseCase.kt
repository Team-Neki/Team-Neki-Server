package com.neki.support.application

import com.neki.common.annotation.UseCase
import com.neki.support.dto.AppVersionCommand
import com.neki.support.service.AppVersionService
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
