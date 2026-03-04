package com.neki.user.application.port

import com.neki.user.contract.OauthInfoPayload
import com.neki.user.domain.enums.Platform
import com.neki.user.domain.enums.ProviderType

/**
 * fileName       : OidcTokenValidatorPort
 * author         : darren
 * date           : 2026. 1. 14. 17:41
 * description    :
 */
interface OidcTokenValidatorPort {
    fun validateIdToken(idToken: String, providerType: ProviderType, platform: Platform): OauthInfoPayload
}
