package com.neki.api.user.api.dto

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern

/**
 * fileName       : AuthRequest
 * author         : koo
 * date           : 2026. 7. 21.
 * description    : 인증/인가 관련 요청 DTO
 */
object AuthRequest {
    @Schema(name = "CreateAuthRequest")
    data class CreateAuth(
        @field:NotBlank(message = "ID 토큰은 필수 입니다")
        val idToken: String?,
        @field:Pattern(regexp = "^(android|ios)$", message = "플랫폼은 android 또는 ios만 가능합니다")
        val platform: String? = null,
    )

    @Schema(name = "RefreshTokenRequest")
    data class RefreshToken(@field:NotBlank(message = "Refresh 토큰은 필수입니다") val refreshToken: String?)
}
