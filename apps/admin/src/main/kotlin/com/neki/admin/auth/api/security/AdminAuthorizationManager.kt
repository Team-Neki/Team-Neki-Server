package com.neki.admin.auth.api.security

import com.neki.admin.auth.application.AdminAccountFacade
import org.springframework.security.authorization.AuthorizationDecision
import org.springframework.security.authorization.AuthorizationManager
import org.springframework.security.core.Authentication
import org.springframework.security.web.access.intercept.RequestAuthorizationContext
import org.springframework.stereotype.Component
import java.util.function.Supplier

@Component
class AdminAuthorizationManager(private val accounts: AdminAccountFacade) :
    AuthorizationManager<RequestAuthorizationContext> {
    override fun check(
        authentication: Supplier<Authentication>,
        context: RequestAuthorizationContext,
    ): AuthorizationDecision {
        val auth: Authentication = authentication.get()
        val principal: AdminPrincipal = auth.principal as? AdminPrincipal ?: return AuthorizationDecision(false)
        if (!auth.isAuthenticated) return AuthorizationDecision(false)
        return AuthorizationDecision(accounts.findActive(principal.userId, principal.subject)?.admin == true)
    }
}
