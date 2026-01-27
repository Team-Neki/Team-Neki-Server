package com.yapp2app.auth.infra.security.handler

import com.fasterxml.jackson.databind.ObjectMapper
import com.yapp2app.common.api.dto.BaseResponse
import com.yapp2app.common.api.dto.ResultCode
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.MediaType
import org.springframework.security.core.AuthenticationException
import org.springframework.security.web.AuthenticationEntryPoint
import org.springframework.stereotype.Component

@Component
class CustomAuthenticationEntryPoint(private val objectMapper: ObjectMapper) : AuthenticationEntryPoint {

    override fun commence(
        request: HttpServletRequest,
        response: HttpServletResponse,
        authException: AuthenticationException,
    ) {
        response.contentType = MediaType.APPLICATION_JSON_VALUE
        response.characterEncoding = Charsets.UTF_8.name()
        response.status = HttpServletResponse.SC_FORBIDDEN

        val errorResponse = BaseResponse<Unit>(
            resultCode = ResultCode.MISSING_TOKEN_ERROR.code,
            message = ResultCode.MISSING_TOKEN_ERROR.message,
            data = null,
        )

        objectMapper.writeValue(response.writer, errorResponse)
    }
}
