package com.neki.api.user.infra.security.oauth.oidc

import com.neki.api.user.infra.security.oauth.dto.OIDCPublicKeysPayload
import com.neki.domain.user.models.ProviderType

/**
 * fileName       : Oidc
 * author         : darren
 * date           : 2025. 12. 31.
 * description    : OAuth 외부 연동을 위한 Port
 */
interface Oidc {
    /**
     * 구현체가 담당하는 OAuth Provider 유형
     */
    val providerType: ProviderType

    /**
     * 카카오 OIDC 공개키 조회
     */
    fun getOIDCPublicKey(): OIDCPublicKeysPayload
}
