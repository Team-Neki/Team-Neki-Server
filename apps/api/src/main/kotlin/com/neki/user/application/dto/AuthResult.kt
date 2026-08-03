package com.neki.user.application.dto

/**
 * fileName       : AuthResult
 * author         : koo
 * date           : 2026. 7. 21.
 * description    : 인증/인가 관련 result
 */
object AuthResult {
    data class GetOauthLogin(val accessToken: String, val refreshToken: String, val isNewUser: Boolean)

    data class GetRefreshToken(val accessToken: String, val refreshToken: String)
}
