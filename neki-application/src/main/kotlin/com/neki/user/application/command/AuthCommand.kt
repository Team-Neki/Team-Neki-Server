package com.neki.user.application.command

import com.neki.user.enums.Platform
import com.neki.user.enums.ProviderType

/**
 * fileName       : AuthCommand
 * author         : darren
 * date           : 2025. 12. 12. 13:18
 * description    : 인증/인가 관련 API
 */
data class RegisterOauthUserCommand(val idToken: String, val providerType: ProviderType, val platform: Platform)

data class RefreshTokenCommand(val refreshToken: String)
