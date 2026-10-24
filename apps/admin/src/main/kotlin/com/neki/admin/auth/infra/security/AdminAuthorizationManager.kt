package com.neki.admin.auth.infra.security

import com.neki.admin.auth.application.AdminAccountService
import com.neki.domain.user.models.User
import org.springframework.security.authorization.AuthorizationDecision
import org.springframework.security.authorization.AuthorizationManager
import org.springframework.security.core.Authentication
import org.springframework.security.web.access.intercept.RequestAuthorizationContext
import org.springframework.stereotype.Component
import java.util.function.Supplier

@Component
class AdminAuthorizationManager(private val accounts: AdminAccountService) :
    AuthorizationManager<RequestAuthorizationContext> {
    override fun check(
        authentication: Supplier<Authentication>,
        context: RequestAuthorizationContext,
    ): AuthorizationDecision {
        val auth: Authentication = authentication.get()
        val principal: AdminPrincipal = auth.principal as? AdminPrincipal ?: return AuthorizationDecision(false)
        if (!auth.isAuthenticated) return AuthorizationDecision(false)
        val user: User = accounts.findActive(principal.userId, principal.subject) ?: return AuthorizationDecision(false)
        return AuthorizationDecision(accounts.isAdmin(user))
    }
}
