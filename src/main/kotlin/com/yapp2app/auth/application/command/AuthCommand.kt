package com.yapp2app.auth.application.command

import com.yapp2app.user.domain.enums.ProviderType

/**
 * fileName       : AuthCommand
 * author         : darren
 * date           : 2025. 12. 12. 13:18
 * description    : 인증/인가 관련 API
 */
data class RegisterKakaoUserCommand(val idToken: String)

data class LoginCommand(val oid: Long, val providerType: ProviderType)

data class RefreshTokenCommand(val refreshToken: String)
