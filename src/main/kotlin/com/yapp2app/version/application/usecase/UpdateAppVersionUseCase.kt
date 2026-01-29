package com.yapp2app.version.application.usecase

import com.yapp2app.common.annotation.UseCase
import com.yapp2app.common.api.dto.ResultCode
import com.yapp2app.common.exception.BusinessException
import com.yapp2app.version.application.command.UpdateAppVersionCommand
import com.yapp2app.version.application.port.AppVersionRepositoryPort
import org.springframework.transaction.annotation.Transactional

/**
 * fileName       : UpdateAppVersionUseCase
 * author         : darren
 * date           : 2026. 1. 29
 * description    : version 변경
 */
@UseCase
class UpdateAppVersionUseCase(private val appVersionRepository: AppVersionRepositoryPort) {

    @Transactional
    fun execute(command: UpdateAppVersionCommand) {
        val appVersion = appVersionRepository.findByPlatform(command.platform)
            ?: throw BusinessException(ResultCode.NOT_FOUND)

        appVersion.minVersion = command.minVersion
        appVersion.currentVersion = command.currentVersion

        appVersionRepository.save(appVersion)
    }
}
