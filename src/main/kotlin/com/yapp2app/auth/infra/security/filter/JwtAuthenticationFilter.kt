package com.yapp2app.auth.infra.security.filter

import com.nimbusds.jose.shaded.gson.JsonArray
import com.nimbusds.jose.shaded.gson.JsonObject
import com.yapp2app.auth.infra.security.token.AuthTokenProvider
import com.yapp2app.common.api.dto.ResultCode
import io.jsonwebtoken.ExpiredJwtException
import io.jsonwebtoken.MalformedJwtException
import io.jsonwebtoken.UnsupportedJwtException
import io.jsonwebtoken.security.SignatureException
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.core.Authentication
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
            val tokenStr: String? = extractToken(request)
            tokenStr?.let {
                val authentication: Authentication = tokenProvider.getAuthentication(it)

                SecurityContextHolder.getContext().authentication = authentication
            }
            filterChain.doFilter(request, response)
        } catch (ex: SignatureException) {
            handleException(response, ResultCode.INVALID_TOKEN_ERROR)
        } catch (ex: SecurityException) {
            handleException(response, ResultCode.INVALID_TOKEN_ERROR)
        } catch (ex: MalformedJwtException) {
            handleException(response, ResultCode.INVALID_TOKEN_ERROR)
        } catch (ex: ExpiredJwtException) {
            handleException(response, ResultCode.EXPIRED_TOKEN_ERROR)
        } catch (ex: UnsupportedJwtException) {
            handleException(response, ResultCode.EXPIRED_TOKEN_ERROR)
        } catch (ex: Exception) {
            handleException(response, ResultCode.SECURITY_ERROR)
        }
    }

    private fun extractToken(request: HttpServletRequest): String? {
        val bearerToken = request.getHeader("Authorization")
        return if (bearerToken?.startsWith("Bearer ") == true) {
            bearerToken.substring(7)
        } else {
            null
        }
    }

    private fun handleException(response: HttpServletResponse, resultCode: ResultCode) {
        var jsonObject = JsonObject()

        response!!.contentType = "application/json;charset=UTF-8"
        response!!.characterEncoding = "utf-8"
        response!!.status = HttpServletResponse.SC_UNAUTHORIZED

        jsonObject.addProperty("resultCode", resultCode.code)
        jsonObject.addProperty("message", resultCode.message)
        jsonObject.addProperty("success", false)
        jsonObject.add("errors", JsonArray())

        response!!.writer.print(jsonObject)
    }
}
