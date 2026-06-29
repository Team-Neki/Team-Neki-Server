package com.neki.user.api.converter

import com.neki.user.api.dto.CreateAuthRequest
import com.neki.user.api.dto.RefreshTokenRequest
import com.neki.user.application.command.RefreshTokenCommand
import com.neki.user.application.command.RegisterOauthUserCommand
import com.neki.user.domain.enums.Platform
import com.neki.user.domain.enums.ProviderType
import org.springframework.stereotype.Component

@Component
class AuthCommandConverter {

    fun toCreateAuthCommand(request: CreateAuthRequest, providerTypeStr: String): RegisterOauthUserCommand {
        val providerType: ProviderType = ProviderType.from(providerTypeStr)
        return RegisterOauthUserCommand(
            idToken = request.idToken!!,
            providerType = providerType,
            platform = Platform.from(request.platform, providerType),
        )
    }

    fun toRefreshTokenCommand(request: RefreshTokenRequest): RefreshTokenCommand =
        RefreshTokenCommand(request.refreshToken!!)
}
