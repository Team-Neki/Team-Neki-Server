package com.yapp2app.auth.infra.oauth

import com.yapp2app.auth.application.port.OidcPort
import com.yapp2app.auth.application.contract.OIDCPublicKeysResponse
import org.springframework.stereotype.Component

/**
 * fileName       : AppleOidcAdapter
 * author         : darren
 * date           : 2025. 12. 31. 10:13
 * description    :
 */
@Component
class AppleOidcAdapter : OidcPort {
    override fun getOIDCPublicKey(): OIDCPublicKeysResponse {
        TODO("Not yet implemented")
    }
}
