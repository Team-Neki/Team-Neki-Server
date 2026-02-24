package com.neki.auth.api.converter

import com.neki.auth.api.dto.GetAuthResponse
import com.neki.auth.application.result.GetAuthResult
import org.springframework.stereotype.Component

@Component
class AuthResultConverter {

    fun toCreateAuthResponse(result: GetAuthResult): GetAuthResponse =
        GetAuthResponse(accessToken = result.accessToken, result.refreshToken)
}
