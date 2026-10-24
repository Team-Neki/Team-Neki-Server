package com.neki.api.common.exception.handler

import com.neki.core.code.ResultCode
import com.neki.core.exception.BusinessException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus

class ExceptionHandlerTest {
    private val handler = ExceptionHandler()

    @Test
    fun logSendFailureIsRetryable() {
        val response = handler.businessExceptionHandler(BusinessException(ResultCode.LOG_SEND_FAILED))

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, response.statusCode)
        assertEquals(ResultCode.LOG_SEND_FAILED.code, response.body?.resultCode)
    }
}
