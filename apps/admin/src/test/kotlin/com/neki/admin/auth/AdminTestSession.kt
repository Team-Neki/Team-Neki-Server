package com.neki.admin.auth

import com.neki.domain.user.infra.persist.jpa.JpaUserRepository
import com.neki.domain.user.infra.security.admin.AdminPrincipal
import com.neki.domain.user.models.ProviderType
import com.neki.domain.user.models.RoleType
import com.neki.domain.user.models.User
import jakarta.servlet.http.Cookie
import org.springframework.security.core.context.SecurityContextImpl
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken
import org.springframework.security.oauth2.core.oidc.OidcIdToken
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser
import org.springframework.security.oauth2.core.oidc.user.OidcUserAuthority
import org.springframework.security.web.context.HttpSessionSecurityContextRepository
import org.springframework.session.Session
import org.springframework.session.SessionRepository
import org.springframework.session.jdbc.JdbcIndexedSessionRepository
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.Instant
import java.util.Base64
import java.util.UUID

object AdminTestSession {
    data class Login(val id: String, val cookie: Cookie)

    fun createUser(users: JpaUserRepository, roles: String = RoleType.USER.role): User = users.saveAndFlush(
        User(
            email = "admin-${UUID.randomUUID()}@example.com",
            name = "관리자",
            oid = "google:${UUID.randomUUID()}",
            roles = roles,
            providerType = ProviderType.ADMIN_SSO,
            profileImageId = null,
        ),
    )

    fun login(repository: JdbcIndexedSessionRepository, user: User): Login {
        val now = Instant.now()
        val idToken = OidcIdToken(
            "test-id-token",
            now,
            now.plusSeconds(3600),
            mapOf("sub" to user.oid!!.removePrefix("google:"), "email" to user.email!!, "email_verified" to true),
        )
        val delegate = DefaultOidcUser(listOf(OidcUserAuthority(idToken)), idToken)
        val principal = AdminPrincipal(user.id!!, user.oid!!, delegate)
        val authentication = OAuth2AuthenticationToken(principal, principal.authorities, "google")

        @Suppress("UNCHECKED_CAST")
        val sessions = repository as SessionRepository<Session>
        val session = sessions.createSession()
        session.setAttribute(
            HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY,
            SecurityContextImpl(authentication),
        )
        sessions.save(session)
        val encoded = Base64.getEncoder().encodeToString(session.id.toByteArray(Charsets.UTF_8))
        return Login(session.id, Cookie("ADMINSESSION", encoded))
    }

    fun csrf(mvc: MockMvc, cookie: Cookie): String = mvc.perform(get("/admin/v1/auth/me").cookie(cookie))
        .andExpect(status().isOk).andReturn().response.getHeader("X-CSRF-TOKEN")!!
}
