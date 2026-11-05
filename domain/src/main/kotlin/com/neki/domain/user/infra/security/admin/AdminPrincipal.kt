package com.neki.domain.user.infra.security.admin

import org.springframework.security.oauth2.core.oidc.user.OidcUser
import org.springframework.security.oauth2.core.user.OAuth2User
import java.io.Serializable

interface AdminAuthenticatedPrincipal {
    val userId: Long
    val identityKey: String
}

class AdminPrincipal(override val userId: Long, override val identityKey: String, private val delegate: OidcUser) :
    AdminAuthenticatedPrincipal,
    OidcUser by delegate,
    Serializable {
    override fun getName(): String = userId.toString()

    companion object {
        private const val serialVersionUID: Long = 1L
    }
}

class AdminOAuth2Principal(
    override val userId: Long,
    override val identityKey: String,
    private val delegate: OAuth2User,
) : AdminAuthenticatedPrincipal,
    OAuth2User by delegate,
    Serializable {
    override fun getName(): String = userId.toString()

    companion object {
        private const val serialVersionUID: Long = 1L
    }
}
