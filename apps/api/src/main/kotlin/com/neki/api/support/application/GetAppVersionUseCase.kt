package com.neki.api.support.application

import com.neki.api.support.application.dto.AppVersionResult
import com.neki.core.annotation.UseCase
import com.neki.domain.support.dto.AppVersionQuery
import com.neki.domain.support.models.AppVersion
import com.neki.domain.support.service.AppVersionService
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
