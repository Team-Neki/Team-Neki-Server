package com.neki.support.service

import com.neki.common.code.ResultCode
import com.neki.common.exception.BusinessException
import com.neki.support.dto.AppVersionCommand
import com.neki.support.dto.AppVersionQuery
import com.neki.support.models.AppVersion
import com.neki.support.repository.AppVersionRepository
import org.springframework.stereotype.Component

/**
 * fileName       : AppVersionService
 * author         : koo
 * date           : 2026. 8. 4.
 * description    : 앱 버전 도메인 서비스
 */
@Component
class AppVersionService(private val appVersionRepository: AppVersionRepository) {

    fun getByPlatform(query: AppVersionQuery.GetAppVersion): AppVersion =
        appVersionRepository.findByPlatform(query.platform)
            ?: throw BusinessException(ResultCode.NOT_FOUND)

    fun updateVersion(command: AppVersionCommand.UpdateAppVersion): AppVersion {
        val appVersion: AppVersion = appVersionRepository.findByPlatform(command.platform)
            ?: throw BusinessException(ResultCode.NOT_FOUND)

        appVersion.updateVersions(command.minVersion, command.currentVersion)

        return appVersionRepository.save(appVersion)
    }
}
