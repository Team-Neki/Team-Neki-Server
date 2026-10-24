package com.neki.domain.user.infra.security.admin

import org.springframework.security.oauth2.core.oidc.user.OidcUser
import java.io.Serializable

class AdminPrincipal(val userId: Long, val identityKey: String, private val delegate: OidcUser) :
    OidcUser by delegate,
    Serializable {
    override fun getName(): String = userId.toString()

    companion object {
        private const val serialVersionUID: Long = 1L
    }
}
