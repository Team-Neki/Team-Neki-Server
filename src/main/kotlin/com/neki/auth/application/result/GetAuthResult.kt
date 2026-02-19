package com.neki.auth.application.result

/**
 * fileName       : AuthResult
 * author         : darren
 * date           : 2025. 12. 29. 14:23
 * description    :
 */

data class GetAuthResult(val accessToken: String, val refreshToken: String)
