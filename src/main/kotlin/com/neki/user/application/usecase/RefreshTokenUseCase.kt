package com.neki.user.application.usecase

import com.neki.common.annotation.UseCase
import com.neki.common.code.ResultCode
import com.neki.common.exception.BusinessException
import com.neki.user.application.dto.AuthCommand
import com.neki.user.application.dto.AuthResult
import com.neki.user.application.port.AuthTokenProviderPort
import com.neki.user.infra.security.token.UserPrincipal
import org.springframework.security.core.Authentication

/**
 * fileName       : RefreshTokenUseCase
 * author         : darren
 * date           : 2026. 1. 2. 18:00
 * description    : RefreshToken으로 AccessToken을 갱신하는 UseCase
 */
@UseCase
class RefreshTokenUseCase(private val tokenProviderPort: AuthTokenProviderPort) {

    fun execute(command: AuthCommand.RefreshToken): AuthResult.GetAuth {
        // 1. RefreshToken 유효성 검증
        if (!tokenProviderPort.validateRefreshToken(command.refreshToken)) {
            throw BusinessException(ResultCode.INVALID_TOKEN_ERROR)
        }

        // 2. RefreshToken에서 사용자 정보 추출
        val authentication: Authentication = tokenProviderPort.getAuthenticationFromRefreshToken(command.refreshToken)
        val userPrincipal = authentication.principal as UserPrincipal

        // 3. 새로운 AccessToken 생성
        val newAccessToken: String = tokenProviderPort.createAccessToken(
            id = userPrincipal.id.toString(),
            roles = userPrincipal.roles.toList(),
            name = userPrincipal.name,
            providerType = userPrincipal.providerType,
        )

        // 4. 새로운 RefreshToken 생성 (Refresh Token Rotation 적용)
        val newRefreshToken: String = tokenProviderPort.createRefreshToken(
            id = userPrincipal.id.toString(),
            roles = userPrincipal.roles.toList(),
            name = userPrincipal.name,
            providerType = userPrincipal.providerType,
        )

        return AuthResult.GetAuth(
            accessToken = newAccessToken,
            refreshToken = newRefreshToken,
        )
    }
}
