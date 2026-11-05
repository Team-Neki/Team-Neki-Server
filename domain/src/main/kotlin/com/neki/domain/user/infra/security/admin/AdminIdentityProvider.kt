package com.neki.domain.user.infra.security.admin

import org.springframework.security.oauth2.core.user.OAuth2User

interface AdminIdentityProvider {
    val registrationId: String

    fun extract(user: OAuth2User): AdminSocialProfile
}

data class AdminSocialProfile(val subject: String, val email: String, val name: String?)
