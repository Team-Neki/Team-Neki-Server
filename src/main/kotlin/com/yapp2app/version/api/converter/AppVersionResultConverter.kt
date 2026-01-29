package com.yapp2app.version.api.converter

import com.yapp2app.version.api.dto.GetAppVersionResponse
import com.yapp2app.version.application.result.GetAppVersionResult
import org.springframework.stereotype.Component

/**
 * fileName       : AppVersionResultConverter
 * author         : darren
 * date           : 2026. 1. 29
 * description    :
 */
@Component
class AppVersionResultConverter {

    fun toGetAppVersionResponse(result: GetAppVersionResult): GetAppVersionResponse = GetAppVersionResponse(
        platform = result.platform.name,
        minVersion = result.minVersion,
        currentVersion = result.currentVersion,
    )
}
