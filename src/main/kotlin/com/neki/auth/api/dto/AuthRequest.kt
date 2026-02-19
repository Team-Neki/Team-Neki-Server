package com.neki.auth.api.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern

/**
 * fileName       : AuthRequest
 * author         : darren
 * date           : 2025. 12. 26. 18:05
 * description    : 인증/인가 관련 요청 body
 */
data class CreateAuthRequest(
    @field:NotBlank(message = "ID 토큰은 필수 입니다")
    val idToken: String?,
    @field:Pattern(regexp = "^(android|ios)$", message = "플랫폼은 android 또는 ios만 가능합니다")
    val platform: String? = null,
)

data class RefreshTokenRequest(@field:NotBlank(message = "Refresh 토큰은 필수입니다") val refreshToken: String?)
