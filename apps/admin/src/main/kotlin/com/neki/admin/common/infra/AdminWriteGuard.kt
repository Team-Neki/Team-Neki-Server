package com.neki.admin.common.infra

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

// 관리자 인증이 연결될 때까지 운영 프로필의 변경 요청을 닫아 둔다.
@Component
@Profile("!test")
class AdminWriteGuard : OncePerRequestFilter() {
    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        if (request.requestURI.startsWith("/admin/v1/") && request.method !in setOf("GET", "HEAD")) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN)
            return
        }
        filterChain.doFilter(request, response)
    }
}
