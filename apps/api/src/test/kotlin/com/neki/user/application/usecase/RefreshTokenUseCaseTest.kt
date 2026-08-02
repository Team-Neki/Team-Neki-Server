package com.neki.user.application.usecase

import com.neki.common.code.ResultCode
import com.neki.common.exception.BusinessException
import com.neki.user.application.dto.AuthCommand
import com.neki.user.application.port.AuthTokenProviderPort
import com.neki.user.enums.ProviderType
import com.neki.user.infra.security.token.UserPrincipal
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.security.core.Authentication

class RefreshTokenUseCaseTest {

    lateinit var tokenProviderPort: AuthTokenProviderPort
    lateinit var useCase: RefreshTokenUseCase

    @BeforeEach
    fun setUp() {
        tokenProviderPort = mockk()
        useCase = RefreshTokenUseCase(tokenProviderPort)
    }

    @Test
    @DisplayName("정상 갱신 - 유효한 refresh token으로 새 토큰 쌍 반환")
    fun `정상 갱신 - 유효한 refresh token으로 새 토큰 쌍 반환`() {
        // Given
        val refreshToken = "valid-refresh-token"
        val userPrincipal = UserPrincipal(
            id = 1L,
            name = "테스트유저",
            providerType = ProviderType.KAKAO,
            email = "test@example.com",
            roles = setOf("USER"),
            password = "NO_PASS",
        )
        val authentication: Authentication = mockk()

        every { tokenProviderPort.validateRefreshToken(refreshToken) } returns true
        every { tokenProviderPort.getAuthenticationFromRefreshToken(refreshToken) } returns authentication
        every { authentication.principal } returns userPrincipal
        every {
            tokenProviderPort.createAccessToken(
                id = "1",
                roles = listOf("USER"),
                name = "테스트유저",
                providerType = ProviderType.KAKAO,
            )
        } returns "new-access-token"
        every {
            tokenProviderPort.createRefreshToken(
                id = "1",
                roles = listOf("USER"),
                name = "테스트유저",
                providerType = ProviderType.KAKAO,
            )
        } returns "new-refresh-token"

        // When
        val result = useCase.execute(AuthCommand.RefreshToken(refreshToken = refreshToken))

        // Then
        result.accessToken shouldBe "new-access-token"
        result.refreshToken shouldBe "new-refresh-token"
        verify(exactly = 1) { tokenProviderPort.validateRefreshToken(refreshToken) }
        verify(exactly = 1) { tokenProviderPort.getAuthenticationFromRefreshToken(refreshToken) }
    }

    @Test
    @DisplayName("유효하지 않은 토큰 - INVALID_TOKEN_ERROR BusinessException 발생")
    fun `유효하지 않은 토큰 - INVALID_TOKEN_ERROR BusinessException 발생`() {
        // Given
        val invalidToken = "invalid-refresh-token"
        every { tokenProviderPort.validateRefreshToken(invalidToken) } returns false

        // When & Then
        val exception = shouldThrow<BusinessException> {
            useCase.execute(AuthCommand.RefreshToken(refreshToken = invalidToken))
        }
        exception.resultCode shouldBe ResultCode.INVALID_TOKEN_ERROR
        verify(exactly = 0) { tokenProviderPort.getAuthenticationFromRefreshToken(any()) }
    }

    @Test
    @DisplayName("만료된 refresh token - 유효하지 않은 토큰과 동일하게 INVALID_TOKEN_ERROR BusinessException 발생")
    fun `만료된 refresh token - 유효하지 않은 토큰과 동일하게 INVALID_TOKEN_ERROR BusinessException 발생`() {
        // Given: 만료된 토큰도 validateRefreshToken이 false를 반환함
        val expiredToken = "expired-refresh-token"
        every { tokenProviderPort.validateRefreshToken(expiredToken) } returns false

        // When & Then
        val exception = shouldThrow<BusinessException> {
            useCase.execute(AuthCommand.RefreshToken(refreshToken = expiredToken))
        }
        exception.resultCode shouldBe ResultCode.INVALID_TOKEN_ERROR
        verify(exactly = 0) { tokenProviderPort.getAuthenticationFromRefreshToken(any()) }
    }
}
