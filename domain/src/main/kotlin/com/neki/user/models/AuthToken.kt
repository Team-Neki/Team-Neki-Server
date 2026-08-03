package com.neki.user.models

/**
 * fileName       : AuthToken
 * author         : koo
 * date           : 2026. 8. 4.
 * description    : 인증 토큰 관련 도메인 객체
 */

/**
 * RefreshToken에서 추출한 사용자 정보
 */
data class TokenPrincipal(val id: Long, val name: String?, val roles: List<String>, val providerType: ProviderType)

/**
 * OIDC idToken 검증으로 확인된 OAuth 사용자 정보
 */
data class OauthUserInfo(
    val providerType: ProviderType,
    val oid: String,
    val email: String?,
    val name: String?,
    val imageUrl: String?,
)

/**
 * 발급된 토큰 쌍
 */
data class IssuedTokens(val accessToken: String, val refreshToken: String)
