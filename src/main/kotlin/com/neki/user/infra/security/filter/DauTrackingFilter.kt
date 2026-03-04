package com.neki.user.infra.security.filter

import com.neki.common.analytics.port.DauTrackingPort
import com.neki.user.infra.security.token.UserPrincipal
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.filter.OncePerRequestFilter

class DauTrackingFilter(private val dauTrackingPort: DauTrackingPort) : OncePerRequestFilter() {

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        val authentication = SecurityContextHolder.getContext().authentication

        val principal = authentication?.principal
        if (authentication?.isAuthenticated == true && principal is UserPrincipal) {
            dauTrackingPort.recordActiveUser(principal.id)
        }

        filterChain.doFilter(request, response)
    }
}
