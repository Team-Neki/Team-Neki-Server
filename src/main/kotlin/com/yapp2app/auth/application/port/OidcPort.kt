package com.yapp2app.auth.application.port

import com.yapp2app.auth.application.contract.OIDCPublicKeysResponse

/**
 * fileName       : OidcPort
 * author         : darren
 * date           : 2025. 12. 31.
 * description    : OAuth 외부 연동을 위한 Port
 */
interface OidcPort {
    /**
     * 카카오 OIDC 공개키 조회
     */
    fun getOIDCPublicKey(): OIDCPublicKeysResponse
}
