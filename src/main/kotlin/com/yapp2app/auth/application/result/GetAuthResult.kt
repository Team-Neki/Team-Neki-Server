package com.yapp2app.auth.application.result

import com.yapp2app.user.domain.enums.ProviderType

/**
 * fileName       : AuthResult
 * author         : darren
 * date           : 2025. 12. 29. 14:23
 * description    :
 */
data class GetAuthResult(val oid: Long, val providerType: ProviderType)

data class GetTokenResult(val accessToken: String, val refreshToken: String)
