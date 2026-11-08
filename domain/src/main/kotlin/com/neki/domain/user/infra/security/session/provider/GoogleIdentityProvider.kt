package com.neki.domain.user.infra.security.session.provider

import com.neki.domain.user.infra.security.session.SocialIdentityProvider
import com.neki.domain.user.infra.security.session.SocialProfile
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.security.oauth2.core.OAuth2AuthenticationException
import org.springframework.security.oauth2.core.OAuth2Error
import org.springframework.security.oauth2.core.oidc.user.OidcUser
import org.springframework.security.oauth2.core.user.OAuth2User
import org.springframework.stereotype.Component

@Component
@ConditionalOnProperty(prefix = "admin.auth", name = ["enabled"], havingValue = "true")
class GoogleIdentityProvider : SocialIdentityProvider {
    override val registrationId: String = "google"

    override fun extract(user: OAuth2User): SocialProfile {
        if (user !is OidcUser) throw OAuth2AuthenticationException(OAuth2Error("invalid_user_info"))
        val subject: String? = user.subject
        val email: String? = user.email
        if (subject == null || email == null || user.emailVerified != true) {
            throw OAuth2AuthenticationException(OAuth2Error("invalid_user_info"))
        }
        return SocialProfile(subject = subject, email = email, name = user.fullName)
    }
}
