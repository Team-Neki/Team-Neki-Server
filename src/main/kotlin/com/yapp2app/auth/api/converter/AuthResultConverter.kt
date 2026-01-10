package com.yapp2app.auth.api.converter

import com.yapp2app.auth.api.dto.GetAuthResponse
import com.yapp2app.auth.application.result.GetAuthResult
import org.springframework.stereotype.Component

@Component
class AuthResultConverter {

    fun toCreateAuthResponse(result: GetAuthResult): GetAuthResponse =
        GetAuthResponse(accessToken = result.accessToken, result.refreshToken)
}
