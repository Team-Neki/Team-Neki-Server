package com.neki.support.application

import com.neki.common.annotation.UseCase
import com.neki.support.application.dto.AppVersionResult
import com.neki.support.dto.AppVersionQuery
import com.neki.support.models.AppVersion
import com.neki.support.service.AppVersionService
import org.springframework.transaction.annotation.Transactional

/**
 * fileName       : GetAppVersionUseCase
 * author         : darren
 * date           : 2026. 1. 29
 * description    : version 조회
 */
@UseCase
class GetAppVersionUseCase(private val appVersionService: AppVersionService) {

    @Transactional(readOnly = true)
    fun execute(query: AppVersionQuery.GetAppVersion): AppVersionResult.GetAppVersion {
        val appVersion: AppVersion = appVersionService.getByPlatform(query)

        return AppVersionResult.GetAppVersion(
            platform = appVersion.platform,
            minVersion = appVersion.minVersion,
            currentVersion = appVersion.currentVersion,
        )
    }
}
