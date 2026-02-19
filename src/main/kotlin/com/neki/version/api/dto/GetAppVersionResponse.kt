package com.neki.version.api.dto

import io.swagger.v3.oas.annotations.media.Schema

/**
 * fileName       : VersionResponse
 * author         : darren
 * date           : 2026. 1. 29
 * description    :
 */
data class GetAppVersionResponse(
    @field:Schema(description = "플랫폼", example = "ANDROID")
    val platform: String,
    @field:Schema(description = "최소 지원 버전", example = "1.0.0")
    val minVersion: String,
    @field:Schema(description = "현재 최신 버전", example = "1.2.0")
    val currentVersion: String,
)
