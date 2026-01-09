package com.yapp2app.auth.infra.oauth

import com.yapp2app.auth.application.port.OauthHelperPort
import com.yapp2app.auth.application.contract.OIDCPublicKeysResponse
import com.yapp2app.auth.application.contract.OauthInfoResponse
import org.springframework.stereotype.Component

/**
 * fileName       : AppleOauthHelper
 * author         : darren
 * date           : 2025. 12. 31. 10:23
 * description    :
 */
@Component
class AppleOauthHelper : OauthHelperPort {
    override fun getOauthInfoByIdToken(idToken: String, publicKeys: OIDCPublicKeysResponse): OauthInfoResponse {
        TODO("Not yet implemented")
    }
}
