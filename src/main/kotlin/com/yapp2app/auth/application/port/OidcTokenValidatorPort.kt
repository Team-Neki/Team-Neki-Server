package com.yapp2app.auth.application.port

import com.yapp2app.auth.application.contract.OauthInfoResponse
import com.yapp2app.auth.domain.Platform
import com.yapp2app.user.domain.enums.ProviderType

/**
 * fileName       : OidcTokenValidatorPort
 * author         : darren
 * date           : 2026. 1. 14. 17:41
 * description    :
 */
interface OidcTokenValidatorPort {
    fun validateIdToken(idToken: String, providerType: ProviderType, platform: Platform): OauthInfoResponse
}
