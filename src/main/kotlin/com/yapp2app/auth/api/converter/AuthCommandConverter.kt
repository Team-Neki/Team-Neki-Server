package com.yapp2app.auth.api.converter

import com.yapp2app.auth.api.dto.CreateAuthRequest
import com.yapp2app.auth.api.dto.RefreshTokenRequest
import com.yapp2app.auth.application.command.RefreshTokenCommand
import com.yapp2app.auth.application.command.RegisterKakaoUserCommand
import org.springframework.stereotype.Component

@Component
class AuthCommandConverter {

    fun toCreateAuthCommand(request: CreateAuthRequest): RegisterKakaoUserCommand =
        RegisterKakaoUserCommand(request.idToken)

    fun toRefreshTokenCommand(request: RefreshTokenRequest): RefreshTokenCommand =
        RefreshTokenCommand(request.refreshToken)
}
