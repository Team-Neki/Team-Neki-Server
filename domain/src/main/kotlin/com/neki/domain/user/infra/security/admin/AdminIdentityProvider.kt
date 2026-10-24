package com.neki.domain.user.infra.security.admin

import org.springframework.security.oauth2.core.oidc.user.OidcUser

interface AdminIdentityProvider {
    val registrationId: String

    fun extract(user: OidcUser): AdminOidcProfile
}

data class AdminOidcProfile(val subject: String, val email: String, val name: String?)
