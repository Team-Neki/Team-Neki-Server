package com.yapp2app.auth.api.request

/**
 * fileName       : AuthRequest
 * author         : darren
 * date           : 2025. 12. 26. 18:05
 * description    : 인증/인가 관련 요청 body
 */
data class KakaoOIDCLoginRequest(val idToken: String)
