package com.neki.domain.user.infra.security.oauth.helper

import com.neki.domain.user.infra.security.oauth.dto.OIDCPublicKeysPayload
import com.neki.domain.user.models.OauthUserInfo
import com.neki.domain.user.models.Platform
import com.neki.domain.user.models.ProviderType

/**
 * fileName       : OauthHelperPort
 * author         : darren
 * date           : 2025. 12. 31. 10:21
 * description    : OAuth OIDC 검증을 위한 Port
 */
interface OauthHelper {
    /**
     * 구현체가 담당하는 OAuth Provider 유형
     */
    val providerType: ProviderType

    fun getOauthInfoByIdToken(idToken: String, publicKeys: OIDCPublicKeysPayload, platform: Platform): OauthUserInfo
}
