package com.yapp2app.auth.application.port

import com.yapp2app.auth.infra.response.OIDCPublicKeysResponse

/**
 * fileName       : OidcPort
 * author         : darren
 * date           : 2025. 12. 31.
 * description    : OAuth 외부 연동을 위한 Port
 */
interface OidcPort {
    /**
     * 카카오 OIDC 공개키 조회
     * TODO: Redis 연결 시 1주일간 캐싱 처리 필요 (카카오 측에서 요청 트레픽이 많으면 차단하기 때문)
     */
    fun getOIDCPublicKey(): OIDCPublicKeysResponse
}
