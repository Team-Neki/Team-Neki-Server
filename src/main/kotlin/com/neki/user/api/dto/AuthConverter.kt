package com.neki.user.api.dto

import com.neki.user.application.dto.AuthCommand
import com.neki.user.application.dto.AuthResult
import com.neki.user.domain.enums.Platform
import com.neki.user.domain.enums.ProviderType
import org.springframework.stereotype.Component

/**
 * fileName       : AuthConverter
 * author         : koo
 * date           : 2026. 7. 21.
 * description    : Auth api layer converter
 */
object AuthConverter {
    @Component
    class RequestConverter {
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

    @Component
    class ResponseConverter {
        fun toCreateAuthResponse(result: AuthResult.GetAuth): GetAuthResponse =
            GetAuthResponse(accessToken = result.accessToken, result.refreshToken)
    }
}
