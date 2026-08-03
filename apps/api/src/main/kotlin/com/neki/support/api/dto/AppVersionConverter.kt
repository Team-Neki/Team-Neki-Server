package com.neki.support.api.dto

import com.neki.support.application.dto.AppVersionResult
import com.neki.support.dto.AppVersionCommand
import com.neki.support.dto.AppVersionQuery
import com.neki.support.models.Platform
import org.springframework.stereotype.Component

/**
 * fileName       : AppVersionConverter
 * author         : koo
 * date           : 2026. 7. 21.
 * description    : AppVersion api layer converter
 */
object AppVersionConverter {
    @Component
    class RequestConverter {
        fun toGetAppVersionQuery(platformStr: String): AppVersionQuery.GetAppVersion {
            val platformEnum: Platform = Platform.from(platformStr)
            return AppVersionQuery.GetAppVersion(platformEnum)
        }

        fun toUpdateAppVersionCommand(
            platformStr: String,
            request: AppVersionRequest.UpdateAppVersion,
        ): AppVersionCommand.UpdateAppVersion {
            val platformEnum: Platform = Platform.from(platformStr)
            return AppVersionCommand.UpdateAppVersion(
                platform = platformEnum,
                minVersion = request.minVersion,
                currentVersion = request.currentVersion,
            )
        }
    }

    @Component
    class ResponseConverter {
        fun toGetAppVersionResponse(result: AppVersionResult.GetAppVersion): AppVersionResponse.GetAppVersion =
            AppVersionResponse.GetAppVersion(
                platform = result.platform.name,
                minVersion = result.minVersion,
                currentVersion = result.currentVersion,
            )
    }
}
