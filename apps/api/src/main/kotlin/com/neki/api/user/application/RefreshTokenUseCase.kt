package com.neki.api.user.application

import com.neki.api.user.application.dto.AuthResult
import com.neki.core.annotation.UseCase
import com.neki.domain.user.dto.AuthCommand
import com.neki.domain.user.models.IssuedTokens
import com.neki.domain.user.service.AuthService

/**
 * fileName       : RefreshTokenUseCase
 * author         : darren
 * date           : 2026. 1. 2. 18:00
 * description    : RefreshToken으로 AccessToken을 갱신하는 UseCase
 */
@UseCase
class RefreshTokenUseCase(private val authService: AuthService) {

    fun execute(command: AuthCommand.RefreshToken): AuthResult.GetRefreshToken {
        val tokens: IssuedTokens = authService.rotateTokens(command)

        return AuthResult.GetRefreshToken(
            accessToken = tokens.accessToken,
            refreshToken = tokens.refreshToken,
        )
    }
}
