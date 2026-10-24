package com.neki.domain.user.infra.security.admin

import com.neki.domain.user.models.AdminIdentity
import com.neki.domain.user.service.AdminAccountAccess
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService
import org.springframework.security.oauth2.core.OAuth2AuthenticationException
import org.springframework.security.oauth2.core.OAuth2Error
import org.springframework.security.oauth2.core.oidc.user.OidcUser
import org.springframework.stereotype.Component

@Component
class AdminOidcUserService(
    private val accounts: AdminAccountAccess,
    private val providers: List<AdminIdentityProvider>,
) : OAuth2UserService<OidcUserRequest, OidcUser> {
    private val delegate = OidcUserService()

    override fun loadUser(userRequest: OidcUserRequest): OidcUser {
        val provider: AdminIdentityProvider = providers.singleOrNull {
            it.registrationId == userRequest.clientRegistration.registrationId
        } ?: throw OAuth2AuthenticationException(OAuth2Error("unsupported_provider"))
        val oidcUser: OidcUser = delegate.loadUser(userRequest)
        val profile: AdminOidcProfile = provider.extract(oidcUser)
        val identityKey = "${provider.registrationId}:${profile.subject}"
        if (provider.registrationId.isBlank() || profile.subject.isBlank() || identityKey.length > 512 ||
            profile.email.isBlank() || profile.email.length > 255
        ) {
            throw OAuth2AuthenticationException(OAuth2Error("invalid_user_info"))
        }
        val identity = AdminIdentity(key = identityKey, email = profile.email, name = profile.name)
        return AdminPrincipal(accounts.findOrCreate(identity), identity.key, oidcUser)
    }
}
