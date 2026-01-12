package com.yapp2app.auth.application.command

/**
 * fileName       : AuthCommand
 * author         : darren
 * date           : 2025. 12. 12. 13:18
 * description    : 인증/인가 관련 API
 */
data class RegisterKakaoUserCommand(val idToken: String)

data class RefreshTokenCommand(val refreshToken: String)
