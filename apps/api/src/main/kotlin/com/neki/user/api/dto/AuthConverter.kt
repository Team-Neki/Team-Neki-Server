package com.neki.user.api.dto

import com.neki.user.application.dto.AuthResult
import com.neki.user.dto.AuthCommand
import com.neki.user.models.Platform
import com.neki.user.models.ProviderType
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
        fun toCreateAuthCommand(
            request: AuthRequest.CreateAuth,
            providerTypeStr: String,
        ): AuthCommand.RegisterOauthUser {
            val providerType: ProviderType = ProviderType.from(providerTypeStr)
            return AuthCommand.RegisterOauthUser(
                idToken = request.idToken!!,
                providerType = providerType,
                platform = Platform.from(request.platform, providerType),
            )
        }

        fun toRefreshTokenCommand(request: AuthRequest.RefreshToken): AuthCommand.RefreshToken =
            AuthCommand.RefreshToken(request.refreshToken!!)
    }

    @Component
    class ResponseConverter {
        fun toOauthLoginResponse(result: AuthResult.GetOauthLogin): AuthResponse.GetOauthLogin =
            AuthResponse.GetOauthLogin(
                accessToken = result.accessToken,
                refreshToken = result.refreshToken,
                isNewUser = result.isNewUser,
            )

        fun toRefreshTokenResponse(result: AuthResult.GetRefreshToken): AuthResponse.GetRefreshToken =
            AuthResponse.GetRefreshToken(accessToken = result.accessToken, refreshToken = result.refreshToken)
    }
}
