package com.yapp2app.version.api.dto

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank

/**
 * fileName       : VersionRequest
 * author         : darren
 * date           : 2026. 1. 29
 * description    :
 */
data class UpdateAppVersionRequest(
    @field:NotBlank(message = "최소 버전은 필수입니다.")
    @field:Schema(description = "최소 지원 버전", example = "1.0.0")
    val minVersion: String,

    @field:NotBlank(message = "현재 버전은 필수입니다.")
    @field:Schema(description = "현재 최신 버전", example = "1.2.0")
    val currentVersion: String,
)
