package com.yapp2app.auth.api.converter

import com.yapp2app.auth.api.dto.CreateAuthRequest
import com.yapp2app.auth.api.dto.RefreshTokenRequest
import com.yapp2app.auth.application.command.RefreshTokenCommand
import com.yapp2app.auth.application.command.RegisterOauthUserCommand
import com.yapp2app.common.api.dto.ResultCode
import com.yapp2app.common.exception.BusinessException
import com.yapp2app.user.domain.enums.ProviderType
import org.springframework.stereotype.Component

@Component
class AuthCommandConverter {

    fun toCreateAuthCommand(request: CreateAuthRequest, providerType: String): RegisterOauthUserCommand =
        RegisterOauthUserCommand(
            idToken = request.idToken!!,
            providerType = toProviderType(providerType),
        )

    fun toRefreshTokenCommand(request: RefreshTokenRequest): RefreshTokenCommand =
        RefreshTokenCommand(request.refreshToken!!)

    private fun toProviderType(providerType: String): ProviderType = runCatching {
        ProviderType.valueOf(providerType.uppercase())
    }
        .getOrElse { throw BusinessException(ResultCode.INVALID_PARAMETER) }
}
