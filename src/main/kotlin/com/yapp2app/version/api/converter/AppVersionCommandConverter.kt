package com.yapp2app.version.api.converter

import com.yapp2app.common.api.dto.ResultCode
import com.yapp2app.common.exception.BusinessException
import com.yapp2app.version.api.dto.UpdateAppVersionRequest
import com.yapp2app.version.application.command.GetAppVersionCommand
import com.yapp2app.version.application.command.UpdateAppVersionCommand
import com.yapp2app.version.domain.enums.Platform
import org.springframework.stereotype.Component

/**
 * fileName       : AppVersionCommandConverter
 * author         : darren
 * date           : 2026. 1. 29
 * description    :
 */
@Component
class AppVersionCommandConverter {

    fun toGetAppVersionCommand(platform: String): GetAppVersionCommand {
        val platformEnum = toPlatform(platform)
        return GetAppVersionCommand(platformEnum)
    }

    fun toUpdateAppVersionCommand(platform: String, request: UpdateAppVersionRequest): UpdateAppVersionCommand {
        val platformEnum = toPlatform(platform)
        return UpdateAppVersionCommand(
            platform = platformEnum,
            minVersion = request.minVersion,
            currentVersion = request.currentVersion,
        )
    }

    private fun toPlatform(platform: String): Platform = runCatching { Platform.valueOf(platform.uppercase()) }
        .getOrElse { throw BusinessException(ResultCode.INVALID_PARAMETER) }
}
