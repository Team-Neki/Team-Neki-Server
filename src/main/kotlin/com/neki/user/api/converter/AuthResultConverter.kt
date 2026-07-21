package com.neki.user.api.converter

import com.neki.user.api.dto.GetAuthResponse
import com.neki.user.application.dto.AuthResult
import org.springframework.stereotype.Component

@Component
class AuthResultConverter {

    fun toCreateAuthResponse(result: AuthResult.GetAuth): GetAuthResponse =
        GetAuthResponse(accessToken = result.accessToken, result.refreshToken)
}
