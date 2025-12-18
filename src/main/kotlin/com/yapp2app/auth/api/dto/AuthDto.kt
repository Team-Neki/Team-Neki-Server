package com.yapp2app.auth.api.dto

/**
 * fileName       : AuthDto
 * author         : darren
 * date           : 2025. 12. 12. 13:20
 * description    : auth 도메인과 관련된 Request/Response DTO

 */

// ====================================================================================================
// Request DTOs
// ====================================================================================================

/**
 * 카카오 OIDC 로그인
 */
data class KakaoOIDCLoginRequest(val idToken: String)

// ====================================================================================================
// Response DTOs
// ====================================================================================================

/**
 * JWT 토큰 반환
 */
data class TokenResponse(val accessToken: String, val refreshToken: String)
