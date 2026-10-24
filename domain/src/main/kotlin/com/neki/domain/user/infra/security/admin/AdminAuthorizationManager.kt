package com.neki.domain.user.infra.security.admin

import com.neki.domain.user.service.AdminAccountAccess
import org.springframework.security.authorization.AuthorizationDecision
import org.springframework.security.authorization.AuthorizationManager
import org.springframework.security.core.Authentication
import org.springframework.security.web.access.intercept.RequestAuthorizationContext
import org.springframework.stereotype.Component
import java.util.function.Supplier

@Component
class AdminAuthorizationManager(private val accounts: AdminAccountAccess) :
    AuthorizationManager<RequestAuthorizationContext> {
    override fun check(
        authentication: Supplier<Authentication>,
        context: RequestAuthorizationContext,
    ): AuthorizationDecision {
        val auth: Authentication = authentication.get()
        val principal: AdminAuthenticatedPrincipal = auth.principal as? AdminAuthenticatedPrincipal
            ?: return AuthorizationDecision(false)
        if (!auth.isAuthenticated) return AuthorizationDecision(false)
        return AuthorizationDecision(accounts.isAdmin(principal.userId, principal.identityKey))
    }
}
