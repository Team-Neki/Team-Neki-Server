package com.neki.domain.user.infra.security.session

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService
import org.springframework.security.oauth2.core.oidc.user.OidcUser
import org.springframework.stereotype.Component

@Component
@ConditionalOnProperty(prefix = "admin.auth", name = ["enabled"], havingValue = "true")
class SessionOidcUserService(private val identities: SocialIdentityResolver) :
    OAuth2UserService<OidcUserRequest, OidcUser> {
    private val delegate = OidcUserService()

    override fun loadUser(userRequest: OidcUserRequest): OidcUser {
        val oidcUser: OidcUser = delegate.loadUser(userRequest)
        val (userId, identityKey) = identities.resolve(userRequest.clientRegistration.registrationId, oidcUser)
        return OidcSessionPrincipal(userId, identityKey, oidcUser)
    }
}
