package com.neki.user.application.dto

import com.neki.user.enums.Platform
import com.neki.user.enums.ProviderType

/**
 * fileName       : AuthCommand
 * author         : koo
 * date           : 2026. 7. 21.
 * description    : 인증/인가 관련 command
 */
object AuthCommand {
    data class RegisterOauthUser(val idToken: String, val providerType: ProviderType, val platform: Platform)

    data class RefreshToken(val refreshToken: String)
}
