package com.neki.domain.user.infra.security.admin.provider

import com.neki.domain.user.infra.security.admin.AdminIdentityProvider
import com.neki.domain.user.infra.security.admin.AdminOidcProfile
import org.springframework.security.oauth2.core.OAuth2AuthenticationException
import org.springframework.security.oauth2.core.OAuth2Error
import org.springframework.security.oauth2.core.oidc.user.OidcUser
import org.springframework.stereotype.Component

@Component
class GoogleAdminIdentityProvider : AdminIdentityProvider {
    override val registrationId: String = "google"

    override fun extract(user: OidcUser): AdminOidcProfile {
        val subject: String? = user.subject
        val email: String? = user.email
        if (subject == null || email == null || user.emailVerified != true) {
            throw OAuth2AuthenticationException(OAuth2Error("invalid_user_info"))
        }
        return AdminOidcProfile(subject = subject, email = email, name = user.fullName)
    }
}
