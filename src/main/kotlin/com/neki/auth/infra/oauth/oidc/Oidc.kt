package com.neki.auth.infra.oauth.oidc

import com.neki.auth.application.contract.OIDCPublicKeysPayload

/**
 * fileName       : Oidc
 * author         : darren
 * date           : 2025. 12. 31.
 * description    : OAuth 외부 연동을 위한 Port
 */
interface Oidc {
    /**
     * 카카오 OIDC 공개키 조회
     */
    fun getOIDCPublicKey(): OIDCPublicKeysPayload
}
