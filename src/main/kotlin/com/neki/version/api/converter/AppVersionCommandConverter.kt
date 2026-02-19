package com.neki.version.api.converter

import com.neki.version.api.dto.UpdateAppVersionRequest
import com.neki.version.application.command.GetAppVersionCommand
import com.neki.version.application.command.UpdateAppVersionCommand
import com.neki.version.domain.enums.Platform
import org.springframework.stereotype.Component

/**
 * fileName       : AppVersionCommandConverter
 * author         : darren
 * date           : 2026. 1. 29
 * description    :
 */
@Component
class AppVersionCommandConverter {

    fun toGetAppVersionCommand(platformStr: String): GetAppVersionCommand {
        val platformEnum = Platform.from(platformStr)
        return GetAppVersionCommand(platformEnum)
    }

    fun toUpdateAppVersionCommand(platformStr: String, request: UpdateAppVersionRequest): UpdateAppVersionCommand {
        val platformEnum = Platform.from(platformStr)
        return UpdateAppVersionCommand(
            platform = platformEnum,
            minVersion = request.minVersion,
            currentVersion = request.currentVersion,
        )
    }
}
