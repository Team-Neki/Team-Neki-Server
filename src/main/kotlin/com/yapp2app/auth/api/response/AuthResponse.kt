package com.yapp2app.auth.api.response

/**
 * fileName       : AuthResponse
 * author         : darren
 * date           : 2025. 12. 26. 18:05
 * description    : Auth aggregate에 대한 응답
 */
data class TokenResponse(val accessToken: String, val refreshToken: String)

data class GetKakaoTokenResponse(
    val accessToken: String,
    val tokenType: String,
    val refreshToken: String,
    val expiresIn: Int,
    val scope: String? = null,
    val refreshTokenExpiresIn: Int? = null,
    val idToken: String? = null,
)
