package com.yapp2app.auth.api.converter

import com.yapp2app.auth.api.dto.CreateAuthRequest
import com.yapp2app.auth.api.dto.RefreshTokenRequest
import com.yapp2app.auth.application.command.RefreshTokenCommand
import com.yapp2app.auth.application.command.RegisterOauthUserCommand
import com.yapp2app.user.domain.enums.ProviderType
import org.springframework.stereotype.Component

@Component
class AuthCommandConverter {

    fun toCreateAuthCommand(request: CreateAuthRequest, providerTypeStr: String): RegisterOauthUserCommand =
        RegisterOauthUserCommand(
            idToken = request.idToken!!,
            providerType = ProviderType.from(providerTypeStr),
            platform = request.platform ?: "android",
        )

    fun toRefreshTokenCommand(request: RefreshTokenRequest): RefreshTokenCommand =
        RefreshTokenCommand(request.refreshToken!!)
}
