package com.yapp2app.version.application.result

import com.yapp2app.version.domain.enums.Platform

/**
 * fileName       : VersionResult
 * author         : darren
 * date           : 2026. 1. 29
 * description    :
 */
data class GetAppVersionResult(val platform: Platform, val minVersion: String, val currentVersion: String)
