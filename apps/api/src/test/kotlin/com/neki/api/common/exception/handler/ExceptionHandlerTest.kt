package com.neki.api.common.exception.handler

import com.neki.core.code.ResultCode
import com.neki.core.exception.BusinessException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType

class ExceptionHandlerTest {
    private val handler = ExceptionHandler()

    @Test
    fun logSendFailureIsRetryable() {
        val response = handler.businessExceptionHandler(BusinessException(ResultCode.LOG_SEND_FAILED))

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, response.statusCode)
        assertEquals(ResultCode.LOG_SEND_FAILED.code, response.body?.resultCode)
    }

    @Test
    fun oversizedLogCanBeSplit() {
        val response = handler.businessExceptionHandler(BusinessException(ResultCode.LOG_PAYLOAD_TOO_LARGE))

        assertEquals(HttpStatus.PAYLOAD_TOO_LARGE, response.statusCode)
        assertEquals(ResultCode.LOG_PAYLOAD_TOO_LARGE.code, response.body?.resultCode)
    }

    @Test
    fun shareNotFoundIs404WithJson() {
        val response = handler.businessExceptionHandler(BusinessException(ResultCode.SHARE_NOT_FOUND))

        assertEquals(HttpStatus.NOT_FOUND, response.statusCode)
        assertEquals(ResultCode.SHARE_NOT_FOUND.code, response.body?.resultCode)
        assertEquals(MediaType.APPLICATION_JSON, response.headers.contentType)
    }

    @Test
    fun shareExpiredIs410WithJson() {
        val response = handler.businessExceptionHandler(BusinessException(ResultCode.SHARE_EXPIRED))

        assertEquals(HttpStatus.GONE, response.statusCode)
        assertEquals(ResultCode.SHARE_EXPIRED.code, response.body?.resultCode)
        assertEquals(MediaType.APPLICATION_JSON, response.headers.contentType)
    }

    @Test
    fun notFoundIsStill400() {
        val response = handler.businessExceptionHandler(BusinessException(ResultCode.NOT_FOUND))

        assertEquals(HttpStatus.BAD_REQUEST, response.statusCode)
        assertEquals(ResultCode.NOT_FOUND.code, response.body?.resultCode)
    }
}
