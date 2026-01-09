package com.yapp2app.auth.application.port

import com.yapp2app.auth.application.contract.OIDCPublicKeysResponse
import com.yapp2app.auth.application.contract.OauthInfoResponse

/**
 * fileName       : OauthHelperPort
 * author         : darren
 * date           : 2025. 12. 31. 10:21
 * description    : OAuth OIDC 검증을 위한 Port
 */
interface OauthHelperPort {
    fun getOauthInfoByIdToken(idToken: String, publicKeys: OIDCPublicKeysResponse): OauthInfoResponse
}
