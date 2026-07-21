package com.neki.support.application.usecase

import com.neki.common.annotation.UseCase
import com.neki.common.code.ResultCode
import com.neki.common.exception.BusinessException
import com.neki.support.application.dto.AppVersionQuery
import com.neki.support.application.dto.AppVersionResult
import com.neki.support.application.port.AppVersionRepositoryPort
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
    fun execute(query: AppVersionQuery.GetAppVersion): AppVersionResult.GetAppVersion {
        val appVersion: AppVersion = appVersionRepository.findByPlatform(query.platform)
            ?: throw BusinessException(ResultCode.NOT_FOUND)

        return AppVersionResult.GetAppVersion(
            platform = appVersion.platform,
            minVersion = appVersion.minVersion,
            currentVersion = appVersion.currentVersion,
        )
    }
}
