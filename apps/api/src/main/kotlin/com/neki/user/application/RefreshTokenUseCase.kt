package com.neki.user.application

import com.neki.common.annotation.UseCase
import com.neki.user.application.dto.AuthResult
import com.neki.user.dto.AuthCommand
import com.neki.user.models.IssuedTokens
import com.neki.user.service.AuthService

/**
 * fileName       : RefreshTokenUseCase
 * author         : darren
 * date           : 2026. 1. 2. 18:00
 * description    : RefreshToken으로 AccessToken을 갱신하는 UseCase
 */
@UseCase
class RefreshTokenUseCase(private val authService: AuthService) {

    fun execute(command: AuthCommand.RefreshToken): AuthResult.GetAuth {
        val tokens: IssuedTokens = authService.rotateTokens(command)

        return AuthResult.GetAuth(
            accessToken = tokens.accessToken,
            refreshToken = tokens.refreshToken,
        )
    }
}
