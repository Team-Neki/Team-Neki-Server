package com.neki.user.api.converter

import com.neki.user.api.dto.CreateAuthRequest
import com.neki.user.api.dto.RefreshTokenRequest
import com.neki.user.application.command.RefreshTokenCommand
import com.neki.user.application.command.RegisterOauthUserCommand
import com.neki.user.enums.Platform
import com.neki.user.enums.ProviderType
import org.springframework.stereotype.Component

@Component
class AuthCommandConverter {

    fun toCreateAuthCommand(request: CreateAuthRequest, providerTypeStr: String): RegisterOauthUserCommand =
        RegisterOauthUserCommand(
            idToken = request.idToken!!,
            providerType = ProviderType.from(providerTypeStr),
            platform = Platform.from(request.platform),
        )

    fun toRefreshTokenCommand(request: RefreshTokenRequest): RefreshTokenCommand =
        RefreshTokenCommand(request.refreshToken!!)
}
