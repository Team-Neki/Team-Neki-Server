package com.neki.auth.infra.oauth.helper

import com.neki.auth.application.contract.OIDCPublicKeysResponse
import com.neki.auth.application.contract.OauthInfoResponse
import com.neki.auth.domain.Platform

/**
 * fileName       : OauthHelperPort
 * author         : darren
 * date           : 2025. 12. 31. 10:21
 * description    : OAuth OIDC 검증을 위한 Port
 */
interface OauthHelper {
    fun getOauthInfoByIdToken(
        idToken: String,
        publicKeys: OIDCPublicKeysResponse,
        platform: Platform,
    ): OauthInfoResponse
}
