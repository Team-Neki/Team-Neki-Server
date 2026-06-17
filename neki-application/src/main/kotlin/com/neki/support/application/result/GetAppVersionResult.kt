package com.neki.support.application.result

import com.neki.support.enums.Platform

/**
 * fileName       : VersionResult
 * author         : darren
 * date           : 2026. 1. 29
 * description    :
 */
data class GetAppVersionResult(val platform: Platform, val minVersion: String, val currentVersion: String)
