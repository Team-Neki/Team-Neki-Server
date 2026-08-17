package com.neki.api.support.api.dto

import io.swagger.v3.oas.annotations.media.Schema

/**
 * fileName       : AppVersionResponse
 * author         : koo
 * date           : 2026. 7. 21.
 * description    : 앱 버전 관련 응답 DTO
 */
object AppVersionResponse {
    @Schema(name = "GetAppVersionResponse")
    data class GetAppVersion(
        @field:Schema(description = "플랫폼", example = "ANDROID")
        val platform: String,
        @field:Schema(description = "최소 지원 버전", example = "1.0.0")
        val minVersion: String,
        @field:Schema(description = "현재 최신 버전", example = "1.2.0")
        val currentVersion: String,
    )
}
