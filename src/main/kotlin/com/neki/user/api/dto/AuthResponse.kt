package com.neki.user.api.dto

import io.swagger.v3.oas.annotations.media.Schema

/**
 * fileName       : AuthResponse
 * author         : darren
 * date           : 2025. 12. 26. 18:05
 * description    : Auth aggregate에 대한 응답
 */
data class GetAuthResponse(
    @field:Schema(description = "AccessToken", example = "ey...")
    val accessToken: String,
    @field:Schema(description = "RefreshToken", example = "ey...")
    val refreshToken: String,
)

/**
 * REST_API TEST용 DTO
 */
data class GetKakaoTokenResponse(
    val accessToken: String,
    val tokenType: String,
    val refreshToken: String,
    val expiresIn: Int,
    val scope: String? = null,
    val refreshTokenExpiresIn: Int? = null,
    val idToken: String? = null,
)
