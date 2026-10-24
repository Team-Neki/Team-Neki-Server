package com.neki.domain.user.infra.security.admin

import com.fasterxml.jackson.databind.ObjectMapper
import com.neki.core.api.dto.BaseResponse
import com.neki.core.code.ResultCode
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.dao.DataAccessException
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.authentication.AuthenticationServiceException
import org.springframework.security.core.Authentication
import org.springframework.security.core.AuthenticationException
import org.springframework.security.web.AuthenticationEntryPoint
import org.springframework.security.web.access.AccessDeniedHandler
import org.springframework.security.web.authentication.AuthenticationFailureHandler
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler
import org.springframework.stereotype.Component

@Component
@ConditionalOnProperty(prefix = "admin.auth", name = ["enabled"], havingValue = "true")
class AdminSecurityResponses(private val mapper: ObjectMapper) :
    AuthenticationEntryPoint,
    AccessDeniedHandler,
    AuthenticationFailureHandler,
    LogoutSuccessHandler {
    private val log = LoggerFactory.getLogger(javaClass)

    override fun commence(
        request: HttpServletRequest,
        response: HttpServletResponse,
        exception: AuthenticationException,
    ) {
        if (exception is AuthenticationServiceException && exception.cause is DataAccessException) {
            log.error("[DATABASE_ERROR] admin authorization failed", exception)
            write(response, 503, ResultCode.ERROR, ResultCode.ERROR.message)
        } else {
            write(response, 401, ResultCode.SECURITY_ERROR, "로그인이 필요합니다.")
        }
    }

    override fun handle(request: HttpServletRequest, response: HttpServletResponse, exception: AccessDeniedException) =
        write(response, 403, ResultCode.ACCESS_DENIED_ERROR, "접근 권한이 없거나 요청 검증에 실패했습니다.")

    override fun onAuthenticationFailure(
        request: HttpServletRequest,
        response: HttpServletResponse,
        exception: AuthenticationException,
    ) = write(response, 401, ResultCode.SECURITY_ERROR, "소셜 로그인에 실패했습니다.")

    override fun onLogoutSuccess(
        request: HttpServletRequest,
        response: HttpServletResponse,
        authentication: Authentication?,
    ) = write(response, 200, ResultCode.SUCCESS, ResultCode.SUCCESS.message)

    private fun write(response: HttpServletResponse, status: Int, code: ResultCode, message: String) {
        response.status = status
        response.contentType = "application/json"
        response.characterEncoding = "UTF-8"
        response.setHeader("Cache-Control", "no-store")
        mapper.writeValue(response.outputStream, BaseResponse<Any>(resultCode = code.code, message = message))
    }
}
