package com.yapp2app.auth.api.converter

import com.yapp2app.auth.api.dto.GetAuthResponse
import com.yapp2app.auth.api.dto.GetTokenResponse
import com.yapp2app.auth.application.result.GetAuthResult
import com.yapp2app.auth.application.result.GetTokenResult
import org.springframework.stereotype.Component

@Component
class AuthResultConverter {

    fun toCreateAuthResponse(result: GetAuthResult): GetAuthResponse =
        GetAuthResponse(oid = result.oid, providerType = result.providerType)

    fun toLoginAuthResponse(result: GetTokenResult): GetTokenResponse =
        GetTokenResponse(accessToken = result.accessToken, result.refreshToken)
}
