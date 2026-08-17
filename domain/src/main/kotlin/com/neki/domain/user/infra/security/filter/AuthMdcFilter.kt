package com.neki.domain.user.infra.security.filter

import com.neki.domain.user.infra.security.token.UserPrincipal
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.MDC
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.filter.OncePerRequestFilter

/**
 * fileName       : AuthMdcFilter
 * author         : koo
 * date           : 2025. 12. 31.
 * description    : 인증 후에 실행되어 인증된 사용자 정보를 MDC에 추가하는 필터
 *                  SecurityContext에서 인증 정보를 가져와 userId를 MDC에 설정
 *                  SecurityFilterChain 내부에서 JwtAuthenticationFilter 다음에 실행
 */
class AuthMdcFilter : OncePerRequestFilter() {

    companion object {
        const val USER_ID = "userId"
    }

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        val authentication = SecurityContextHolder.getContext().authentication

        val principal = authentication?.principal
        if (authentication?.isAuthenticated == true && principal is UserPrincipal) {
            MDC.put(USER_ID, principal.id.toString())
        }

        filterChain.doFilter(request, response)
        // RequestMdcFilter에서 MDC Context clear
    }
}
