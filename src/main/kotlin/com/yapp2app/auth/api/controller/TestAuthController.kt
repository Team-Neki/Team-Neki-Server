package com.yapp2app.auth.api.controller

import com.yapp2app.auth.api.request.LoginRequest
import com.yapp2app.auth.api.response.GetTokenResponse
import com.yapp2app.auth.infra.security.token.AuthTokenProvider
import com.yapp2app.common.api.dto.BaseResponse
import com.yapp2app.common.api.dto.ResultCode
import com.yapp2app.common.exception.BusinessException
import com.yapp2app.user.domain.enums.ProviderType
import com.yapp2app.user.infra.persist.jpa.UserRepository
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * fileName       : TestAuthController
 * author         : koo
 * date           : 2025. 12. 28. 오후 7:58
 * description    :
 */
@Deprecated("로컬 토큰 발급을 위한 임시 엔드 포인트")
@RestController
@RequestMapping("/api/auth/test")
class TestAuthController(
    private val tokenProvider: AuthTokenProvider,
    private val userRepository: UserRepository,
    private val passwordEncoder: PasswordEncoder,
) {

    @PostMapping("/login")
    fun login(@RequestBody request: LoginRequest): BaseResponse<GetTokenResponse> {
        val user = userRepository.findByOidAndProviderType(
            request.oid,
            ProviderType.KAKAO,
        ) ?: throw BusinessException(ResultCode.NOT_FOUND_USER)

        // Access Token 생성
        val accessToken = tokenProvider.createToken(
            id = user.id.toString(),
            roles = user.roles.split(","),
            providerType = ProviderType.LOCAL,
        )

        // Refresh Token 생성
        val refreshToken = tokenProvider.createToken(
            id = user.id.toString(),
            roles = user.roles.split(","),
            providerType = ProviderType.LOCAL,
        )

        return BaseResponse(
            data = GetTokenResponse(
                accessToken = accessToken,
                refreshToken = refreshToken,
            ),
        )
    }
}
