package com.neki.support.application.usecase

import com.neki.common.annotation.UseCase
import com.neki.common.code.ResultCode
import com.neki.common.exception.BusinessException
import com.neki.support.application.command.GetAppVersionCommand
import com.neki.support.application.port.AppVersionRepositoryPort
import com.neki.support.application.result.GetAppVersionResult
import com.neki.support.domain.entity.AppVersion
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
        val appVersion: AppVersion = appVersionRepository.findByPlatform(command.platform)
            ?: throw BusinessException(ResultCode.NOT_FOUND)

        return GetAppVersionResult(
            platform = appVersion.platform,
            minVersion = appVersion.minVersion,
            currentVersion = appVersion.currentVersion,
        )
    }
}
