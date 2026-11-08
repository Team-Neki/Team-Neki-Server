package com.neki.domain.user.infra.security.session

import com.neki.domain.user.dto.UserCommand
import com.neki.domain.user.service.AdminAccountAccess
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.dao.DataAccessException
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService
import org.springframework.security.oauth2.core.OAuth2AuthenticationException
import org.springframework.security.oauth2.core.OAuth2Error
import org.springframework.security.oauth2.core.oidc.user.OidcUser
import org.springframework.stereotype.Component

// 관리자 로그인은 Google만 사용하므로 Google이 확인한 이메일로 계정을 식별한다.
@Component
@ConditionalOnProperty(prefix = "admin.auth", name = ["enabled"], havingValue = "true")
class SessionOidcUserService(private val accounts: AdminAccountAccess) : OAuth2UserService<OidcUserRequest, OidcUser> {
    private val delegate = OidcUserService()

    override fun loadUser(userRequest: OidcUserRequest): OidcUser {
        val oidcUser: OidcUser = delegate.loadUser(userRequest)
        val email: String? = oidcUser.email
        if (email.isNullOrBlank() || email.length > 255 || oidcUser.emailVerified != true) {
            throw OAuth2AuthenticationException(OAuth2Error("invalid_user_info"))
        }
        val userId: Long = try {
            accounts.findOrCreate(UserCommand.RegisterAdminAccount(email = email, name = oidcUser.fullName))
        } catch (exception: DataAccessException) {
            throw OAuth2AuthenticationException(OAuth2Error("admin_database_unavailable"), exception)
        }
        return SessionPrincipal(userId, oidcUser)
    }
}
