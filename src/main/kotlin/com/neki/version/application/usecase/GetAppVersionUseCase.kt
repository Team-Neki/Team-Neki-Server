package com.neki.version.application.usecase

import com.neki.common.annotation.UseCase
import com.neki.common.api.dto.ResultCode
import com.neki.common.exception.BusinessException
import com.neki.version.application.command.GetAppVersionCommand
import com.neki.version.application.port.AppVersionRepositoryPort
import com.neki.version.application.result.GetAppVersionResult
import org.springframework.transaction.annotation.Transactional

/**
 * fileName       : GetAppVersionUseCase
 * author         : darren
 * date           : 2026. 1. 29
 * description    : version 조회
 */
@UseCase
class GetAppVersionUseCase(private val appVersionRepository: AppVersionRepositoryPort) {

    @Transactional(readOnly = true)
    fun execute(command: GetAppVersionCommand): GetAppVersionResult {
        val appVersion = appVersionRepository.findByPlatform(command.platform)
            ?: throw BusinessException(ResultCode.NOT_FOUND)

        return GetAppVersionResult(
            platform = appVersion.platform,
            minVersion = appVersion.minVersion,
            currentVersion = appVersion.currentVersion,
        )
    }
}
