package com.neki.api.user.infra.security.handler

import com.fasterxml.jackson.databind.ObjectMapper
import com.neki.core.api.dto.BaseResponse
import com.neki.core.code.ResultCode
import io.jsonwebtoken.ExpiredJwtException
import io.jsonwebtoken.MalformedJwtException
import io.jsonwebtoken.UnsupportedJwtException
import io.jsonwebtoken.security.SignatureException
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.MediaType
import org.springframework.security.core.AuthenticationException
import org.springframework.security.web.AuthenticationEntryPoint
import org.springframework.stereotype.Component

/**
 * 인증 실패 시 에러 응답 처리
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
 * ### D-996 (MISSING_TOKEN_ERROR) - 토큰 없음
 * - **원인**: Authorization 헤더에 토큰이 없습니다.
 * - **대응**: 로그인이 필요합니다.
 *
 * ### D-999 (SECURITY_ERROR) - 인증 실패
 * - **원인**: 예상하지 못한 인증 오류가 발생했습니다.
 * - **대응**: 재로그인이 필요합니다. `/api/auth/login` API를 호출하세요.
 * - **재로그인 필요 여부**: 예
 */
@Component
class CustomAuthenticationEntryPoint(private val objectMapper: ObjectMapper) : AuthenticationEntryPoint {

    override fun commence(
        request: HttpServletRequest,
        response: HttpServletResponse,
        authException: AuthenticationException,
    ) {
        val (resultCode, status) = resolveError(authException)

        response.contentType = MediaType.APPLICATION_JSON_VALUE
        response.characterEncoding = Charsets.UTF_8.name()
        response.status = status

        val errorResponse = BaseResponse<Unit>(
            resultCode = resultCode.code,
            message = resultCode.message,
            data = null,
        )

        objectMapper.writeValue(response.writer, errorResponse)
    }

    private fun resolveError(authException: AuthenticationException): Pair<ResultCode, Int> =
        when (val cause = authException.cause) {
            is ExpiredJwtException -> ResultCode.EXPIRED_TOKEN_ERROR to HttpServletResponse.SC_UNAUTHORIZED
            is SignatureException -> ResultCode.INVALID_TOKEN_ERROR to HttpServletResponse.SC_FORBIDDEN
            is SecurityException -> ResultCode.INVALID_TOKEN_ERROR to HttpServletResponse.SC_FORBIDDEN
            is MalformedJwtException -> ResultCode.INVALID_TOKEN_ERROR to HttpServletResponse.SC_FORBIDDEN
            is UnsupportedJwtException -> ResultCode.INVALID_TOKEN_ERROR to HttpServletResponse.SC_UNAUTHORIZED
            null -> ResultCode.MISSING_TOKEN_ERROR to HttpServletResponse.SC_FORBIDDEN
            else -> ResultCode.SECURITY_ERROR to HttpServletResponse.SC_FORBIDDEN
        }
}
