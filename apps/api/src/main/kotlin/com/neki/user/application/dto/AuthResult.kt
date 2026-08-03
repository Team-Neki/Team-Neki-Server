package com.neki.user.application.dto

/**
 * fileName       : AuthResult
 * author         : koo
 * date           : 2026. 8. 3. 오전 2:19
 * description    : 인증/인가 관련 result
 */
object AuthResult {
    data class GetAuth(val accessToken: String, val refreshToken: String)

    /**
     * idToken 발급 테스트용 result
     */
    data class KakaoToken(
        val accessToken: String,
        val tokenType: String,
        val refreshToken: String,
        val expiresIn: Int,
        val scope: String? = null,
        val refreshTokenExpiresIn: Int? = null,
        val idToken: String? = null,
    )
}
