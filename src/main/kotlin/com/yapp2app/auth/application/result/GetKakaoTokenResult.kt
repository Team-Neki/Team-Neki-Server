package com.yapp2app.auth.application.result

/**
 * fileName       : AuthResult
 * author         : darren
 * date           : 2025. 12. 26. 18:20
 * description    : Auth usercase 관련 result
 */
data class GetKakaoTokenResult(
    val accessToken: String,
    val tokenType: String,
    val refreshToken: String,
    val expiresIn: Int,
    val scope: String? = null,
    val refreshTokenExpiresIn: Int? = null,
    val idToken: String? = null,
)
