package com.neki.user.api.converter

import com.neki.user.api.dto.CreateAuthRequest
import com.neki.user.api.dto.RefreshTokenRequest
import com.neki.user.application.dto.AuthCommand
import com.neki.user.domain.enums.Platform
import com.neki.user.domain.enums.ProviderType
import org.springframework.stereotype.Component

@Component
class AuthCommandConverter {

    fun toCreateAuthCommand(request: CreateAuthRequest, providerTypeStr: String): AuthCommand.RegisterOauthUser {
        val providerType: ProviderType = ProviderType.from(providerTypeStr)
        return AuthCommand.RegisterOauthUser(
            idToken = request.idToken!!,
            providerType = providerType,
            platform = Platform.from(request.platform, providerType),
        )
    }

    fun toRefreshTokenCommand(request: RefreshTokenRequest): AuthCommand.RefreshToken =
        AuthCommand.RefreshToken(request.refreshToken!!)
}
