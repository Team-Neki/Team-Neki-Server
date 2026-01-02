package com.yapp2app.auth.api.request

import com.yapp2app.user.domain.enums.ProviderType
import jakarta.validation.constraints.NotBlank

/**
 * fileName       : AuthRequest
 * author         : darren
 * date           : 2025. 12. 26. 18:05
 * description    : 인증/인가 관련 요청 body
 */
data class KakaoRegisterRequest(@NotBlank(message = "ID 토큰은 필수입니다") val idToken: String)

data class LoginRequest(val oid: Long, val providerType: ProviderType)

data class RefreshTokenRequest(@NotBlank(message = "Refresh 토큰은 필수입니다") val refreshToken: String)
