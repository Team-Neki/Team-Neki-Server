package com.neki.user.external

import com.neki.user.models.ProviderType
import com.neki.user.models.TokenPrincipal

interface AuthTokenProvider {
    fun createAccessToken(id: String, name: String?, roles: Collection<String>, providerType: ProviderType): String

    fun createRefreshToken(id: String, name: String?, roles: Collection<String>, providerType: ProviderType): String

    fun getPrincipalFromRefreshToken(token: String): TokenPrincipal

    fun validateRefreshToken(token: String): Boolean
}
