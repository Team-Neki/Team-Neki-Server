package com.neki.support.application.dto

import com.neki.support.domain.enums.Platform

/**
 * fileName       : AppVersionResult
 * author         : koo
 * date           : 2026. 7. 21.
 * description    : App version result
 */
object AppVersionResult {
    data class GetAppVersion(val platform: Platform, val minVersion: String, val currentVersion: String)
}
