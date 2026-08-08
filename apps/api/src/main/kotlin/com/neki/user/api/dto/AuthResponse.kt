package com.neki.user.api.dto

import io.swagger.v3.oas.annotations.media.Schema

/**
 * fileName       : AuthResponse
 * author         : koo
 * date           : 2026. 7. 21.
 * description    : 인증/인가 관련 응답 DTO
 */
object AuthResponse {
    @Schema(name = "GetOauthLoginResponse")
    data class GetOauthLogin(
        @field:Schema(description = "AccessToken", example = "ey...")
        val accessToken: String,
        @field:Schema(description = "RefreshToken", example = "ey...")
        val refreshToken: String,
        @field:Schema(description = "이번 로그인으로 새로 가입된 신규 유저 여부", example = "true")
        val isNewUser: Boolean,
    )

    @Schema(name = "GetRefreshTokenResponse")
    data class GetRefreshToken(
        @field:Schema(description = "AccessToken", example = "ey...")
        val accessToken: String,
        @field:Schema(description = "RefreshToken", example = "ey...")
        val refreshToken: String,
    )

    /**
     * REST_API TEST용 DTO
     */
    @Schema(name = "GetKakaoTokenResponse")
    data class GetKakaoToken(
        val accessToken: String,
        val tokenType: String,
        val refreshToken: String,
        val expiresIn: Int,
        val scope: String? = null,
        val refreshTokenExpiresIn: Int? = null,
        val idToken: String? = null,
    )
}
