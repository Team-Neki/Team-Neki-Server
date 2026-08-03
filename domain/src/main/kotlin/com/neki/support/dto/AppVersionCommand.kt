package com.neki.support.dto

import com.neki.support.models.Platform

/**
 * fileName       : AppVersionCommand
 * author         : koo
 * date           : 2026. 7. 21.
 * description    : App version command
 */
object AppVersionCommand {
    data class UpdateAppVersion(val platform: Platform, val minVersion: String, val currentVersion: String)
}
