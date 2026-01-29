package com.yapp2app.version.application.usecase

import com.yapp2app.common.annotation.UseCase
import com.yapp2app.common.api.dto.ResultCode
import com.yapp2app.common.exception.BusinessException
import com.yapp2app.version.application.command.GetAppVersionCommand
import com.yapp2app.version.application.port.AppVersionRepositoryPort
import com.yapp2app.version.application.result.GetAppVersionResult
import org.springframework.transaction.annotation.Transactional

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
