package com.yapp2app.auth.infra.security.filter

import com.yapp2app.auth.infra.security.token.AuthTokenProvider
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

/**
 * fileName       : JwtAuthenticationFilter
 * author         : koo
 * date           : 2025. 12. 28. 오후 8:19
 * description    : JWT 토큰 검증 필터 (임시 구현)
 */
@Component
class JwtAuthenticationFilter(private val tokenProvider: AuthTokenProvider) : OncePerRequestFilter() {

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        try {
            // Authorization 헤더에서 토큰 추출
            val token = extractToken(request)

            // 토큰이 있으면 검증 후 인증 정보 설정
            if (token != null) {
                val authentication = tokenProvider.getAuthentication(token)
                SecurityContextHolder.getContext().authentication = authentication
            }
        } catch (e: Exception) {
            logger.debug("JWT validation failed: ${e.message}")
            return
        }

        filterChain.doFilter(request, response)
    }

    private fun extractToken(request: HttpServletRequest): String? {
        val bearerToken = request.getHeader("Authorization")
        return if (bearerToken?.startsWith("Bearer ") == true) {
            bearerToken.substring(7)
        } else {
            null
        }
    }
}
