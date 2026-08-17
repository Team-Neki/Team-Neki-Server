package com.neki.domain.support.dto

import com.neki.domain.support.models.Platform

/**
 * fileName       : AppVersionCommand
 * author         : koo
 * date           : 2026. 7. 21.
 * description    : App version command
 */
object AppVersionCommand {
    data class UpdateAppVersion(val platform: Platform, val minVersion: String, val currentVersion: String)
}
