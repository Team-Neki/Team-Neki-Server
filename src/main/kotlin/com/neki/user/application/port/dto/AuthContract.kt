package com.neki.user.application.port.dto

import com.neki.user.domain.enums.ProviderType

/**
 * fileName       : AuthContract
 * author         : koo
 * date           : 2026. 7. 22.
 * description    : 인증/인가 port 계약 타입
 */
object AuthContract {
    /**
     * OAuth 사용자 정보 추출 결과
     */
    data class OauthInfoPayload(
        val providerType: ProviderType,
        val oid: String,
        val email: String?,
        val name: String?,
        val imageUrl: String?,
    )

    data class OIDCPublicKeysPayload(val keys: MutableList<OIDCPublicKey>)

    data class OIDCPublicKey(val kid: String, val alg: String, val use: String, val n: String, val e: String)

    /**
     * idToken 발급 테스트용 payload
     */
    data class KakaoTokenPayload(
        val accessToken: String,
        val tokenType: String,
        val refreshToken: String,
        val expiresIn: Int,
        val scope: String? = null,
        val refreshTokenExpiresIn: Int? = null,
        val idToken: String? = null,
    )
}
