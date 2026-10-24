package com.neki.domain.user.infra.security.admin

import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService
import org.springframework.security.oauth2.core.user.OAuth2User
import org.springframework.stereotype.Component

@Component
class AdminOAuth2UserService(private val identities: AdminSocialIdentityResolver) :
    OAuth2UserService<OAuth2UserRequest, OAuth2User> {
    private val delegate = DefaultOAuth2UserService()

    override fun loadUser(userRequest: OAuth2UserRequest): OAuth2User {
        val user: OAuth2User = delegate.loadUser(userRequest)
        val (userId, identityKey) = identities.resolve(userRequest.clientRegistration.registrationId, user)
        return AdminOAuth2Principal(userId, identityKey, user)
    }
}
