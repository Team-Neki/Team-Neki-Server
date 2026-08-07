package com.neki.user.external

import com.neki.user.models.OauthUserInfo
import com.neki.user.models.Platform
import com.neki.user.models.ProviderType

/**
 * fileName       : OidcTokenValidator
 * author         : darren
 * date           : 2026. 1. 14. 17:41
 * description    :
 */
interface OidcTokenValidator {
    fun validateIdToken(idToken: String, providerType: ProviderType, platform: Platform): OauthUserInfo
}
