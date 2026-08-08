package com.neki.api.user.infra.security.handler

import com.fasterxml.jackson.databind.ObjectMapper
import com.neki.core.api.dto.BaseResponse
import com.neki.core.code.ResultCode
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.MediaType
import org.springframework.security.web.access.AccessDeniedHandler
import org.springframework.stereotype.Component

/**
 * fileName       : CustomAccessDeniedHandler
 * author         : darren
 * date           : 2026. 1. 27. 11:06
 * description    :
 */
@Component
class CustomAccessDeniedHandler(private val objectMapper: ObjectMapper) : AccessDeniedHandler {

    override fun handle(
        request: HttpServletRequest,
        response: HttpServletResponse,
        accessDeniedException: org.springframework.security.access.AccessDeniedException?,
    ) {
        response.contentType = MediaType.APPLICATION_JSON_VALUE
        response.characterEncoding = Charsets.UTF_8.name()
        response.status = HttpServletResponse.SC_FORBIDDEN

        val errorResponse = BaseResponse<Unit>(
            resultCode = ResultCode.ACCESS_DENIED_ERROR.code,
            message = ResultCode.ACCESS_DENIED_ERROR.message,
            data = null,
        )

        objectMapper.writeValue(response.writer, errorResponse)
    }
}
