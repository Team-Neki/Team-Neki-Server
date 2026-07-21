package com.neki.support.api.converter

import com.neki.support.api.dto.UpdateAppVersionRequest
import com.neki.support.application.dto.AppVersionCommand
import com.neki.support.application.dto.AppVersionQuery
import com.neki.support.domain.enums.Platform
import org.springframework.stereotype.Component

/**
 * fileName       : AppVersionCommandConverter
 * author         : darren
 * date           : 2026. 1. 29
 * description    :
 */
@Component
class AppVersionCommandConverter {

    fun toGetAppVersionQuery(platformStr: String): AppVersionQuery.GetAppVersion {
        val platformEnum: Platform = Platform.from(platformStr)
        return AppVersionQuery.GetAppVersion(platformEnum)
    }

    fun toUpdateAppVersionCommand(
        platformStr: String,
        request: UpdateAppVersionRequest,
    ): AppVersionCommand.UpdateAppVersion {
        val platformEnum: Platform = Platform.from(platformStr)
        return AppVersionCommand.UpdateAppVersion(
            platform = platformEnum,
            minVersion = request.minVersion,
            currentVersion = request.currentVersion,
        )
    }
}
