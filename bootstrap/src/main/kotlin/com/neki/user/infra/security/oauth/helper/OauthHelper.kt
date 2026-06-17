package com.neki.user.infra.security.oauth.helper

import com.neki.user.application.contract.OIDCPublicKeysPayload
import com.neki.user.application.contract.OauthInfoPayload
import com.neki.user.domain.enums.Platform

/**
 * fileName       : OauthHelperPort
 * author         : darren
 * date           : 2025. 12. 31. 10:21
 * description    : OAuth OIDC 검증을 위한 Port
 */
interface OauthHelper {
    fun getOauthInfoByIdToken(idToken: String, publicKeys: OIDCPublicKeysPayload, platform: Platform): OauthInfoPayload
}
