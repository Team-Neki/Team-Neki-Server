package com.yapp2app.auth.application.usecase

import com.yapp2app.auth.application.command.LoginCommand
import com.yapp2app.auth.application.result.GetTokenResult
import com.yapp2app.auth.infra.security.token.AuthTokenProvider
import com.yapp2app.common.annotation.UseCase
import com.yapp2app.common.api.dto.ResultCode
import com.yapp2app.common.exception.BusinessException
import com.yapp2app.user.application.port.UserRepositoryPort
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.AuthenticationException

/**
 * fileName       : LoginUseCase
 * author         : darren
 * date           : 2025. 12. 30. 18:05
 * description    : 스프링 시큐리티를 사용한 로그인 UseCase
 */
@UseCase
class LoginUseCase(
    private val tokenProvider: AuthTokenProvider,
    private val userRepositoryPort: UserRepositoryPort,
    private val authenticationManager: AuthenticationManager,
) {

    fun execute(loginCommand: LoginCommand): GetTokenResult {
        // 1. 스프링 시큐리티 인증 수행
        // username 형식: "oid:providerType", password: providerType의 name
        val username = "${loginCommand.oid}:${loginCommand.providerType}"
        val providerType = loginCommand.providerType.name

        try {
            authenticationManager.authenticate(
                UsernamePasswordAuthenticationToken(username, providerType),
            )

            // 2. 인증 성공 후 사용자 조회
            val user = userRepositoryPort.findByOid(loginCommand.oid, loginCommand.providerType)
                ?: throw BusinessException(ResultCode.NOT_FOUND_USER)

            // 3. JWT 토큰 생성
            val roles = user.roles.split(",").map { it.trim() }
            val accessToken = tokenProvider.createToken(
                id = user.id.toString(),
                roles = roles,
                providerType = user.providerType,
            )
            // TODO: refreshToken은 별도 만료 시간으로 생성하도록 개선 필요
            val refreshToken = tokenProvider.createToken(
                id = user.id.toString(),
                roles = roles,
                providerType = user.providerType,
            )

            return GetTokenResult(
                accessToken = accessToken,
                refreshToken = refreshToken,
            )
        } catch (e: AuthenticationException) {
            throw BusinessException(ResultCode.SECURITY_ERROR)
        }
    }
}
