package com.neki.user.service

import com.neki.common.code.ResultCode
import com.neki.common.exception.BusinessException
import com.neki.user.dto.AuthCommand
import com.neki.user.external.AuthTokenProvider
import com.neki.user.external.OidcTokenValidator
import com.neki.user.models.IssuedTokens
import com.neki.user.models.OauthUserInfo
import com.neki.user.models.TokenPrincipal
import com.neki.user.models.User
import org.springframework.stereotype.Component

/**
 * fileName       : AuthService
 * author         : koo
 * date           : 2026. 8. 3. 오전 2:03
 * description    : 인증 토큰 도메인 서비스
 */
@Component
class AuthService(
    private val tokenProviderPort: AuthTokenProvider,
    private val oidcTokenValidatorPort: OidcTokenValidator,
) {

    /**
     * OIDC idToken 검증 후 OAuth 사용자 정보 추출
     */
    fun validateOauthToken(command: AuthCommand.RegisterOauthUser): OauthUserInfo =
        oidcTokenValidatorPort.validateIdToken(command.idToken, command.providerType, command.platform)

    /**
     * 사용자 기준 토큰 쌍 발급
     */
    fun issueTokens(user: User): IssuedTokens {
        val accessToken: String = tokenProviderPort.createAccessToken(
            id = user.id.toString(),
            roles = user.roles.split(","),
            name = user.name,
            providerType = user.providerType,
        )

        val refreshToken: String = tokenProviderPort.createRefreshToken(
            id = user.id.toString(),
            roles = user.roles.split(","),
            name = user.name,
            providerType = user.providerType,
        )

        return IssuedTokens(accessToken = accessToken, refreshToken = refreshToken)
    }

    /**
     * RefreshToken 검증 후 토큰 쌍 재발급 (Refresh Token Rotation 적용)
     */
    fun rotateTokens(command: AuthCommand.RefreshToken): IssuedTokens {
        if (!tokenProviderPort.validateRefreshToken(command.refreshToken)) {
            throw BusinessException(ResultCode.INVALID_TOKEN_ERROR)
        }

        val principal: TokenPrincipal =
            tokenProviderPort.getPrincipalFromRefreshToken(command.refreshToken)

        val newAccessToken: String = tokenProviderPort.createAccessToken(
            id = principal.id.toString(),
            roles = principal.roles,
            name = principal.name,
            providerType = principal.providerType,
        )

        val newRefreshToken: String = tokenProviderPort.createRefreshToken(
            id = principal.id.toString(),
            roles = principal.roles,
            name = principal.name,
            providerType = principal.providerType,
        )

        return IssuedTokens(accessToken = newAccessToken, refreshToken = newRefreshToken)
    }
}
