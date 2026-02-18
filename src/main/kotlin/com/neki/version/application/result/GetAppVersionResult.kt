package com.neki.version.application.result

import com.neki.version.domain.enums.Platform

/**
 * fileName       : VersionResult
 * author         : darren
 * date           : 2026. 1. 29
 * description    :
 */
data class GetAppVersionResult(val platform: Platform, val minVersion: String, val currentVersion: String)
