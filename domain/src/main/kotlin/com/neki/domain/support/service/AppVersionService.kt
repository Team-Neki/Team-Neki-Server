package com.neki.domain.support.service

import com.neki.core.code.ResultCode
import com.neki.core.exception.BusinessException
import com.neki.domain.support.dto.AppVersionCommand
import com.neki.domain.support.dto.AppVersionQuery
import com.neki.domain.support.models.AppVersion
import com.neki.domain.support.repository.AppVersionRepository
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
