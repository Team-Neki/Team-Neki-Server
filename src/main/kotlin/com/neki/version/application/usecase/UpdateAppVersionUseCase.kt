package com.neki.version.application.usecase

import com.neki.common.annotation.UseCase
import com.neki.common.api.dto.ResultCode
import com.neki.common.exception.BusinessException
import com.neki.version.application.command.UpdateAppVersionCommand
import com.neki.version.application.port.AppVersionRepositoryPort
import com.neki.version.domain.entity.AppVersion
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
        val appVersion: AppVersion = appVersionRepository.findByPlatform(command.platform)
            ?: throw BusinessException(ResultCode.NOT_FOUND)

        appVersion.minVersion = command.minVersion
        appVersion.currentVersion = command.currentVersion

        appVersionRepository.save(appVersion)
    }
}
