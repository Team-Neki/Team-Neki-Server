package com.neki.admin.common.exception.handler

import com.neki.core.code.ResultCode
import com.neki.core.exception.BusinessException
import com.neki.core.exception.dto.ExceptionMsg
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.validation.FieldError
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException

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

    @ExceptionHandler(HttpMessageNotReadableException::class, MethodArgumentTypeMismatchException::class)
    fun invalidRequestHandler(ex: Exception): ResponseEntity<ExceptionMsg> = ResponseEntity(
        ExceptionMsg(
            resultCode = ResultCode.INVALID_PARAMETER.code,
            message = ResultCode.INVALID_PARAMETER.message,
        ),
        HttpStatus.BAD_REQUEST,
    )

    @ExceptionHandler(Exception::class)
    fun exceptionHandler(ex: Exception): ResponseEntity<ExceptionMsg> {
        log.error("[SYSTEM_ERROR] unhandled exception", ex)

        return ResponseEntity(
            ExceptionMsg(resultCode = ResultCode.ERROR.code, message = ResultCode.ERROR.message),
            HttpStatus.BAD_REQUEST,
        )
    }
}
