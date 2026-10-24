package com.neki.admin.auth.infra.security

import com.neki.admin.auth.application.AdminAccountFacade
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService
import org.springframework.security.oauth2.core.OAuth2AuthenticationException
import org.springframework.security.oauth2.core.OAuth2Error
import org.springframework.security.oauth2.core.oidc.user.OidcUser
import org.springframework.stereotype.Component

@Component
class AdminOidcUserService(private val accounts: AdminAccountFacade) : OAuth2UserService<OidcUserRequest, OidcUser> {
    private val delegate = OidcUserService()

    override fun loadUser(userRequest: OidcUserRequest): OidcUser {
        val google: OidcUser = delegate.loadUser(userRequest)
        val email: String? = google.email
        if (userRequest.clientRegistration.registrationId != "google" || google.subject.isNullOrBlank() ||
            email.isNullOrBlank() || email.length > 255 || google.emailVerified != true
        ) {
            throw OAuth2AuthenticationException(OAuth2Error("invalid_user_info"))
        }
        val userId: Long = accounts.findOrCreate(google.subject, email, google.fullName)
        return AdminPrincipal(userId, google)
    }
}
