package com.neki.domain.user.infra.security.admin.provider

import com.neki.domain.user.infra.security.admin.AdminIdentityProvider
import com.neki.domain.user.infra.security.admin.AdminSocialProfile
import org.springframework.security.oauth2.core.OAuth2AuthenticationException
import org.springframework.security.oauth2.core.OAuth2Error
import org.springframework.security.oauth2.core.oidc.user.OidcUser
import org.springframework.security.oauth2.core.user.OAuth2User
import org.springframework.stereotype.Component

@Component
class GoogleAdminIdentityProvider : AdminIdentityProvider {
    override val registrationId: String = "google"

    override fun extract(user: OAuth2User): AdminSocialProfile {
        if (user !is OidcUser) throw OAuth2AuthenticationException(OAuth2Error("invalid_user_info"))
        val subject: String? = user.subject
        val email: String? = user.email
        if (subject == null || email == null || user.emailVerified != true) {
            throw OAuth2AuthenticationException(OAuth2Error("invalid_user_info"))
        }
        return AdminSocialProfile(subject = subject, email = email, name = user.fullName)
    }
}
