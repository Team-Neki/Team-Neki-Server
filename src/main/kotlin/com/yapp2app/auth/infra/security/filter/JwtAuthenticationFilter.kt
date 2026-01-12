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
            handleException(response, ResultCode.INVALID_TOKEN_ERROR, HttpServletResponse.SC_FORBIDDEN)
        } catch (ex: SecurityException) {
            handleException(response, ResultCode.INVALID_TOKEN_ERROR, HttpServletResponse.SC_FORBIDDEN)
        } catch (ex: MalformedJwtException) {
            handleException(response, ResultCode.INVALID_TOKEN_ERROR, HttpServletResponse.SC_FORBIDDEN)
        } catch (ex: ExpiredJwtException) {
            handleException(response, ResultCode.EXPIRED_TOKEN_ERROR, HttpServletResponse.SC_UNAUTHORIZED)
        } catch (ex: UnsupportedJwtException) {
            handleException(response, ResultCode.EXPIRED_TOKEN_ERROR, HttpServletResponse.SC_UNAUTHORIZED)
        } catch (ex: Exception) {
            handleException(response, ResultCode.SECURITY_ERROR, HttpServletResponse.SC_FORBIDDEN)
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

    /**
     * JWT 검증 실패 시 에러 응답 처리
     *
     * @param response HttpServletResponse
     * @param resultCode 에러 코드
     *
     * ## HTTP Status: 401 Unauthorized
     * 인증이 필요하거나 인증에 실패한 경우 반환됩니다.
     *
     * ## 에러 코드별 클라이언트 대응 방법
     *
     * ### D-997 (EXPIRED_TOKEN_ERROR) - 토큰 만료
     * - **원인**: AccessToken의 유효기간이 만료되었습니다.
     * - **대응**: RefreshToken을 사용하여 `/api/auth/refresh` API를 호출해 새로운 토큰을 발급받으세요.
     * - **재로그인 필요 여부**: 아니오 (RefreshToken이 유효한 경우)
     *
     * ### D-998 (INVALID_TOKEN_ERROR) - 토큰 무효
     * - **원인**: 토큰의 서명이 올바르지 않거나, 토큰 형식이 잘못되었습니다.
     * - **대응**: 재로그인이 필요합니다. `/api/auth/login` API를 호출하세요.
     * - **재로그인 필요 여부**: 예
     *
     * ### D-999 (SECURITY_ERROR) - 인증 실패
     * - **원인**: 예상하지 못한 인증 오류가 발생했습니다.
     * - **대응**: 재로그인이 필요합니다. `/api/auth/login` API를 호출하세요.
     * - **재로그인 필요 여부**: 예
     */
    private fun handleException(response: HttpServletResponse, resultCode: ResultCode, status: Int) {
        val jsonObject = JsonObject()

        response.contentType = "application/json;charset=UTF-8"
        response.characterEncoding = "utf-8"
        response.status = status

        jsonObject.addProperty("resultCode", resultCode.code)
        jsonObject.addProperty("message", resultCode.message)
        jsonObject.addProperty("success", false)
        jsonObject.add("errors", JsonArray())

        response.writer.print(jsonObject)
    }
}
