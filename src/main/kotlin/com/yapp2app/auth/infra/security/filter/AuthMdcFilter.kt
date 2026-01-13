package com.yapp2app.auth.infra.security.filter

import com.yapp2app.auth.infra.security.token.UserPrincipal
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
        // SecurityContext에서 인증 정보 가져오기
        val authentication = SecurityContextHolder.getContext().authentication

        // 인증된 사용자인 경우 MDC에 userId 추가
        if (authentication != null && authentication.isAuthenticated && authentication.principal != "anonymousUser") {
            val userPrincipal = authentication.principal as UserPrincipal

            // DB상 name을 가져오는 코드 (택 1)
            val userId = userPrincipal.id.toString()

            MDC.put(USER_ID, userId)
        }

        filterChain.doFilter(request, response)
    }
}
