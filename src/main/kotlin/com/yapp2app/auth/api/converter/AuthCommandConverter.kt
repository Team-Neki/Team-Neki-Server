package com.yapp2app.auth.api.converter

import com.yapp2app.auth.api.dto.CreateAuthRequest
import com.yapp2app.auth.api.dto.LoginRequest
import com.yapp2app.auth.api.dto.RefreshTokenRequest
import com.yapp2app.auth.application.command.LoginCommand
import com.yapp2app.auth.application.command.RefreshTokenCommand
import com.yapp2app.auth.application.command.RegisterKakaoUserCommand
import org.springframework.stereotype.Component

@Component
class AuthCommandConverter {

    fun toCreateAuthCommand(request: CreateAuthRequest): RegisterKakaoUserCommand =
        RegisterKakaoUserCommand(request.idToken)

    fun toLoginAuthCommand(request: LoginRequest): LoginCommand =
        LoginCommand(oid = request.oid, providerType = request.providerType)

    fun toRefreshTokenCommand(request: RefreshTokenRequest): RefreshTokenCommand =
        RefreshTokenCommand(request.refreshToken)
}
