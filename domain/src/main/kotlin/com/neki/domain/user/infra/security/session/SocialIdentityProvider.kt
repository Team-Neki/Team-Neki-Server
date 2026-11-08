package com.neki.domain.user.infra.security.session

import org.springframework.security.oauth2.core.user.OAuth2User

interface SocialIdentityProvider {
    val registrationId: String

    fun extract(user: OAuth2User): SocialProfile
}

data class SocialProfile(val subject: String, val email: String, val name: String?)
