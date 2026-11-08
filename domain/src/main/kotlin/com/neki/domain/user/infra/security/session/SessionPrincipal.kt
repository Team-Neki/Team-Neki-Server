package com.neki.domain.user.infra.security.session

import org.springframework.security.oauth2.core.oidc.user.OidcUser
import org.springframework.security.oauth2.core.user.OAuth2User
import java.io.Serializable

interface SessionPrincipal {
    val userId: Long
    val identityKey: String
}

class OidcSessionPrincipal(
    override val userId: Long,
    override val identityKey: String,
    private val delegate: OidcUser,
) : SessionPrincipal,
    OidcUser by delegate,
    Serializable {
    override fun getName(): String = userId.toString()

    companion object {
        private const val serialVersionUID: Long = 1L
    }
}

class OAuth2SessionPrincipal(
    override val userId: Long,
    override val identityKey: String,
    private val delegate: OAuth2User,
) : SessionPrincipal,
    OAuth2User by delegate,
    Serializable {
    override fun getName(): String = userId.toString()

    companion object {
        private const val serialVersionUID: Long = 1L
    }
}
