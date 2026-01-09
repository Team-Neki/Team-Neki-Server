package com.yapp2app.auth.application.usecase

import com.yapp2app.auth.application.command.LoginCommand
import com.yapp2app.auth.application.result.GetTokenResult
import com.yapp2app.auth.infra.security.token.AuthTokenProvider
import com.yapp2app.common.annotation.UseCase
import com.yapp2app.common.api.dto.ResultCode
import com.yapp2app.common.exception.BusinessException
import com.yapp2app.user.application.port.UserRepositoryPort

/**
 * fileName       : LoginUseCase
 * author         : darren
 * date           : 2025. 12. 30. 18:05
 * description    : 스프링 시큐리티를 사용한 로그인 UseCase
 */
@UseCase
class LoginUseCase(private val tokenProvider: AuthTokenProvider, private val userRepositoryPort: UserRepositoryPort) {

    fun execute(loginCommand: LoginCommand): GetTokenResult {
        val user = userRepositoryPort.findByOid(loginCommand.oid, loginCommand.providerType) ?: throw BusinessException(
            ResultCode.NOT_FOUND_USER,
        )

        // JWT 토큰 생성
        val accessToken = tokenProvider.createAccessToken(
            id = user.id.toString(),
            roles = user.roles.split(","),
            name = user.name,
            providerType = user.providerType,
        )

        val refreshToken = tokenProvider.createRefreshToken(
            id = user.id.toString(),
            roles = user.roles.split(","),
            name = user.name,
            providerType = user.providerType,
        )

        return GetTokenResult(
            accessToken = accessToken,
            refreshToken = refreshToken,
        )
    }
}
