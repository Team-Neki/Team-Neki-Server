package com.neki.api.user.infra.security.filter

import com.neki.api.user.infra.security.token.AuthTokenProviderAdapter
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException
import org.springframework.security.core.Authentication
import org.springframework.security.core.AuthenticationException
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.web.AuthenticationEntryPoint
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

/**
 * fileName       : JwtAuthenticationFilter
 * author         : koo
 * date           : 2025. 12. 28. 오후 8:19
 * description    : JWT 토큰 검증 필터
 */
@Component
class JwtAuthenticationFilter(
    private val tokenProvider: AuthTokenProviderAdapter,
    private val authenticationEntryPoint: AuthenticationEntryPoint,
) : OncePerRequestFilter() {

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        try {
            val tokenStr: String? = extractToken(request)
            tokenStr?.let {
                val authentication: Authentication = tokenProvider.getAuthentication(it)
                SecurityContextHolder.getContext().authentication = authentication
            }
            filterChain.doFilter(request, response)
        } catch (ex: AuthenticationException) {
            SecurityContextHolder.clearContext()
            authenticationEntryPoint.commence(request, response, ex)
        } catch (ex: Exception) {
            SecurityContextHolder.clearContext()
            authenticationEntryPoint.commence(
                request,
                response,
                AuthenticationCredentialsNotFoundException("Authentication failed", ex),
            )
        }
    }

    private fun extractToken(request: HttpServletRequest): String? {
        val bearerToken: String? = request.getHeader("Authorization")
        return if (bearerToken?.startsWith("Bearer ") == true) {
            bearerToken.substring(7)
        } else {
            null
        }
    }
}
