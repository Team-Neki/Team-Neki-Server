package com.yapp2app.auth.application.port

import com.yapp2app.user.domain.enums.ProviderType
import org.springframework.security.core.Authentication

interface AuthTokenProviderPort {
    fun createAccessToken(id: String, name: String?, roles: Collection<String>, providerType: ProviderType): String

    fun createRefreshToken(id: String, name: String?, roles: Collection<String>, providerType: ProviderType): String

    fun getAuthenticationFromRefreshToken(token: String): Authentication

    fun validateRefreshToken(token: String): Boolean
}
