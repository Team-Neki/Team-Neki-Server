package com.neki.domain.user.infra.security.admin

import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService
import org.springframework.security.oauth2.core.oidc.user.OidcUser
import org.springframework.stereotype.Component

@Component
class AdminOidcUserService(private val identities: AdminSocialIdentityResolver) :
    OAuth2UserService<OidcUserRequest, OidcUser> {
    private val delegate = OidcUserService()

    override fun loadUser(userRequest: OidcUserRequest): OidcUser {
        val oidcUser: OidcUser = delegate.loadUser(userRequest)
        val (userId, identityKey) = identities.resolve(userRequest.clientRegistration.registrationId, oidcUser)
        return AdminPrincipal(userId, identityKey, oidcUser)
    }
}
