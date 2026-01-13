package com.yapp2app.auth.application.usecase

import com.yapp2app.auth.application.command.RefreshTokenCommand
import com.yapp2app.auth.application.port.AuthTokenProviderPort
import com.yapp2app.auth.application.result.GetAuthResult
import com.yapp2app.auth.infra.security.token.UserPrincipal
import com.yapp2app.common.annotation.UseCase
import com.yapp2app.common.api.dto.ResultCode
import com.yapp2app.common.exception.BusinessException

/**
 * fileName       : RefreshTokenUseCase
 * author         : darren
 * date           : 2026. 1. 2. 18:00
 * description    : RefreshToken으로 AccessToken을 갱신하는 UseCase
 */
@UseCase
class RefreshTokenUseCase(private val tokenProviderPort: AuthTokenProviderPort) {

    fun execute(command: RefreshTokenCommand): GetAuthResult {
        // 1. RefreshToken 유효성 검증
        if (!tokenProviderPort.validateRefreshToken(command.refreshToken)) {
            throw BusinessException(ResultCode.INVALID_TOKEN_ERROR)
        }

        // 2. RefreshToken에서 사용자 정보 추출
        val authentication = tokenProviderPort.getAuthenticationFromRefreshToken(command.refreshToken)
        val userPrincipal = authentication.principal as UserPrincipal

        // 3. 새로운 AccessToken 생성
        val newAccessToken = tokenProviderPort.createAccessToken(
            id = userPrincipal.id.toString(),
            roles = userPrincipal.roles.toList(),
            name = userPrincipal.name,
            providerType = userPrincipal.providerType,
        )

        // 4. 새로운 RefreshToken 생성 (Refresh Token Rotation 적용)
        val newRefreshToken = tokenProviderPort.createRefreshToken(
            id = userPrincipal.id.toString(),
            roles = userPrincipal.roles.toList(),
            name = userPrincipal.name,
            providerType = userPrincipal.providerType,
        )

        return GetAuthResult(
            accessToken = newAccessToken,
            refreshToken = newRefreshToken,
        )
    }
}
