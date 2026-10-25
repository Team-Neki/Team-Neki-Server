package com.neki.admin.common.exception.handler

import com.neki.core.code.ResultCode
import com.neki.core.exception.BusinessException
import com.neki.core.exception.dto.ExceptionMsg
import org.apache.catalina.connector.ClientAbortException
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.validation.FieldError
import org.springframework.web.HttpMediaTypeNotSupportedException
import org.springframework.web.HttpRequestMethodNotSupportedException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.MissingServletRequestParameterException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.context.request.async.AsyncRequestNotUsableException
import org.springframework.web.method.annotation.HandlerMethodValidationException
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException
import org.springframework.web.servlet.NoHandlerFoundException
import org.springframework.web.servlet.resource.NoResourceFoundException

/**
 * fileName       : ExceptionHandler
 * author         : koo
 * date           : 2026. 8. 29.
 * description    : admin 예외 전역처리. apps:api 의 응답 형식(ExceptionMsg)과 상태 코드 규칙을 따른다
 */
@RestControllerAdvice
class ExceptionHandler {
    private val log: Logger = LoggerFactory.getLogger(javaClass)

    @ExceptionHandler(BusinessException::class)
    fun businessExceptionHandler(ex: BusinessException): ResponseEntity<ExceptionMsg> {
        log.warn("[BUSINESS_ERROR] code={} | message={}", ex.resultCode.code, ex.resultCode.message)

        return ResponseEntity(
            ExceptionMsg(resultCode = ex.resultCode.code, message = ex.resultCode.message),
            HttpStatus.BAD_REQUEST,
        )
    }

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun methodValidExceptionHandler(ex: MethodArgumentNotValidException): ResponseEntity<ExceptionMsg> {
        val firstMessage: String = ex.bindingResult.allErrors
            .firstOrNull()
            ?.let { (it as? FieldError)?.defaultMessage }
            ?: ResultCode.INVALID_PARAMETER.message

        return ResponseEntity(
            ExceptionMsg(resultCode = ResultCode.INVALID_PARAMETER.code, message = firstMessage),
            HttpStatus.BAD_REQUEST,
        )
    }

    @ExceptionHandler(
        HttpMessageNotReadableException::class,
        MethodArgumentTypeMismatchException::class,
        MissingServletRequestParameterException::class,
        HandlerMethodValidationException::class,
    )
    fun invalidRequestHandler(ex: Exception): ResponseEntity<ExceptionMsg> = ResponseEntity(
        ExceptionMsg(
            resultCode = ResultCode.INVALID_PARAMETER.code,
            message = ResultCode.INVALID_PARAMETER.message,
        ),
        HttpStatus.BAD_REQUEST,
    )

    // 클라이언트가 응답 전에 연결을 끊은 경우. 이미 닫힌 응답에 쓰면 2차 예외가 나므로 반환하지 않는다
    @ExceptionHandler(ClientAbortException::class, AsyncRequestNotUsableException::class)
    fun clientAbortHandler(ex: Exception) {
        log.debug("[CLIENT_ABORT] client closed connection before response was written: {}", ex.message)
    }

    // 대부분 봇·스캐너의 경로 탐색이라 catch-all 의 ERROR 로그로 보내지 않는다
    @ExceptionHandler(NoResourceFoundException::class, NoHandlerFoundException::class)
    fun notFoundHandler(ex: Exception): ResponseEntity<ExceptionMsg> {
        log.debug("[NOT_FOUND] no handler for request: {}", ex.message)

        return ResponseEntity(
            ExceptionMsg(resultCode = ResultCode.NOT_FOUND.code, message = ResultCode.NOT_FOUND.message),
            HttpStatus.NOT_FOUND,
        )
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException::class, HttpMediaTypeNotSupportedException::class)
    fun unsupportedRequestHandler(ex: Exception): ResponseEntity<ExceptionMsg> {
        log.debug("[UNSUPPORTED_REQUEST] {}", ex.message)

        val status: HttpStatus = if (ex is HttpRequestMethodNotSupportedException) {
            HttpStatus.METHOD_NOT_ALLOWED
        } else {
            HttpStatus.UNSUPPORTED_MEDIA_TYPE
        }

        return ResponseEntity(
            ExceptionMsg(
                resultCode = ResultCode.INVALID_PARAMETER.code,
                message = ResultCode.INVALID_PARAMETER.message,
            ),
            status,
        )
    }

    @ExceptionHandler(Exception::class)
    fun exceptionHandler(ex: Exception): ResponseEntity<ExceptionMsg> {
        log.error("[SYSTEM_ERROR] unhandled exception", ex)

        return ResponseEntity(
            ExceptionMsg(resultCode = ResultCode.ERROR.code, message = ResultCode.ERROR.message),
            HttpStatus.INTERNAL_SERVER_ERROR,
        )
    }
}
