package com.neki.auth.application.port

import com.neki.auth.application.contract.OauthInfoResponse
import com.neki.auth.domain.Platform
import com.neki.user.domain.enums.ProviderType

/**
 * fileName       : OidcTokenValidatorPort
 * author         : darren
 * date           : 2026. 1. 14. 17:41
 * description    :
 */
interface OidcTokenValidatorPort {
    fun validateIdToken(idToken: String, providerType: ProviderType, platform: Platform): OauthInfoResponse
}
