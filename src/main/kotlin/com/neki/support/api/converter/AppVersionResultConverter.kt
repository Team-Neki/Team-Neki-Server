package com.neki.support.api.converter

import com.neki.support.api.dto.GetAppVersionResponse
import com.neki.support.application.dto.AppVersionResult
import org.springframework.stereotype.Component

/**
 * fileName       : AppVersionResultConverter
 * author         : darren
 * date           : 2026. 1. 29
 * description    :
 */
@Component
class AppVersionResultConverter {

    fun toGetAppVersionResponse(result: AppVersionResult.GetAppVersion): GetAppVersionResponse = GetAppVersionResponse(
        platform = result.platform.name,
        minVersion = result.minVersion,
        currentVersion = result.currentVersion,
    )
}
