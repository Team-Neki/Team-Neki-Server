package com.neki.auth.api.converter

import com.neki.auth.api.dto.CreateAuthRequest
import com.neki.auth.api.dto.RefreshTokenRequest
import com.neki.auth.application.command.RefreshTokenCommand
import com.neki.auth.application.command.RegisterOauthUserCommand
import com.neki.auth.domain.Platform
import com.neki.user.domain.enums.ProviderType
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
