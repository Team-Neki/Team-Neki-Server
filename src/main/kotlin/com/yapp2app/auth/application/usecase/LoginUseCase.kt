package com.yapp2app.auth.application.usecase

import com.yapp2app.auth.application.command.LoginCommand
import com.yapp2app.auth.application.result.GetTokenResult
import com.yapp2app.auth.infra.security.token.AuthTokenProvider
import com.yapp2app.auth.infra.security.token.UserPrincipal
import com.yapp2app.common.annotation.UseCase
import com.yapp2app.common.api.dto.ResultCode
import com.yapp2app.common.exception.BusinessException
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
    private val authenticationManager: AuthenticationManager,
) {

    fun execute(loginCommand: LoginCommand): GetTokenResult {
        // 1. 스프링 시큐리티 인증 수행
        // username 형식: "oid:providerType", password: OAuth는 실제 비밀번호가 없으므로 "NO_PASS" 사용
        val username = "${loginCommand.oid}:${loginCommand.providerType}"
        val password = "NO_PASS"

        try {
            // 인증 수행 (CustomUserDetailsService에서 DB 조회 발생)
            val authentication = authenticationManager.authenticate(
                UsernamePasswordAuthenticationToken(username, password),
            )

            // 인증 결과에서 UserPrincipal 추출
            val userPrincipal = authentication.principal as UserPrincipal

            // JWT 토큰 생성
            val accessToken = tokenProvider.createAccessToken(
                id = userPrincipal.id.toString(),
                roles = userPrincipal.roles.toList(),
                name = userPrincipal.name,
                providerType = userPrincipal.providerType,
            )

            val refreshToken = tokenProvider.createRefreshToken(
                id = userPrincipal.id.toString(),
                roles = userPrincipal.roles.toList(),
                name = userPrincipal.name,
                providerType = userPrincipal.providerType,
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
