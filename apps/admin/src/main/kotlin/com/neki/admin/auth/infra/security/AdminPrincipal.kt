package com.neki.admin.auth.infra.security

import org.springframework.security.oauth2.core.oidc.user.OidcUser
import java.io.Serializable

class AdminPrincipal(val userId: Long, private val delegate: OidcUser) :
    OidcUser by delegate,
    Serializable {
    override fun getName(): String = userId.toString()

    companion object {
        private const val serialVersionUID: Long = 1L
    }
}
