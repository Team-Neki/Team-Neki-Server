package com.neki.common.exception.handler

import com.neki.common.api.dto.ResultCode
import com.neki.common.exception.BusinessException
import com.neki.common.exception.dto.ExceptionMsg
import com.neki.common.exception.dto.FieldErrorDetail
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.validation.FieldError
import org.springframework.validation.ObjectError
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.MissingServletRequestParameterException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.context.request.WebRequest
import org.springframework.web.method.annotation.HandlerMethodValidationException
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException
import java.util.function.Consumer

/**
 * fileName       : ExceptionHandler
 * author         : darren
 * date           : 2025. 12. 12. 13:27
 * description    : 예외 전역처리 클래스
 */
@RestControllerAdvice
class ExceptionHandler {
    private val log: Logger = LoggerFactory.getLogger(javaClass)

    @ExceptionHandler(BusinessException::class)
    fun businessExceptionHandler(ex: BusinessException): ResponseEntity<ExceptionMsg> {
        log.warn("[BUSINESS_ERROR] code={} | message={}", ex.resultCode.code, ex.resultCode.message)

        if (ex.resultCode == ResultCode.INVALID_TOKEN_ERROR) {
            return ResponseEntity(
                ExceptionMsg(
                    resultCode = ex.resultCode.code,
                    message = ex.resultCode.message,
                ),
                HttpStatus.FORBIDDEN,
            )
        }

        val temp = ResponseEntity(
            ExceptionMsg(
                resultCode = ex.resultCode.code,
                message = ex.resultCode.message,
            ),
            HttpStatus.BAD_REQUEST,
        )

        return temp
    }

    @ExceptionHandler(Exception::class)
    fun exceptionHandler(ex: Exception): ResponseEntity<ExceptionMsg> {
        log.error("[SYSTEM_ERROR] unhandled exception", ex)

        val temp = ResponseEntity(
            ExceptionMsg(
                resultCode = ResultCode.ERROR.code,
                message = ResultCode.ERROR.message,
            ),
            HttpStatus.BAD_REQUEST,
        )

        return temp
    }

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun methodValidExceptionHandler(ex: MethodArgumentNotValidException): ResponseEntity<ExceptionMsg> {
        val errors: MutableList<FieldErrorDetail> = ArrayList<FieldErrorDetail>()
        ex.bindingResult.allErrors.forEach(
            Consumer { error: ObjectError ->
                errors.add(
                    FieldErrorDetail(
                        field = (error as FieldError).field,
                        message = error.defaultMessage ?: "Invalid Params",
                    ),
                )
            },
        )

        return ResponseEntity(
            ExceptionMsg(
                resultCode = ResultCode.INVALID_PARAMETER.code,
                message = errors.get(0).message,
            ),
            HttpStatus.BAD_REQUEST,
        )
    }

    @ExceptionHandler(HttpMessageNotReadableException::class)
    fun handleHttpMessageNotReadableExceptionHandler(
        ex: HttpMessageNotReadableException,
    ): ResponseEntity<ExceptionMsg> = ResponseEntity(
        ExceptionMsg(
            resultCode = ResultCode.INVALID_PARAMETER.code,
            message = ResultCode.INVALID_PARAMETER.message,
        ),
        HttpStatus.BAD_REQUEST,
    )

    @ExceptionHandler(MissingServletRequestParameterException::class)
    fun handleMissingServletRequestParameterExceptionHandler(
        ex: MissingServletRequestParameterException,
        request: WebRequest,
    ): ResponseEntity<ExceptionMsg> = ResponseEntity(
        ExceptionMsg(
            resultCode = ResultCode.INVALID_PARAMETER.code,
            message = ResultCode.INVALID_PARAMETER.message,
        ),
        HttpStatus.BAD_REQUEST,
    )

    @ExceptionHandler(HandlerMethodValidationException::class)
    fun handleMethodValidationExceptionHandler(ex: HandlerMethodValidationException): ResponseEntity<ExceptionMsg> =
        ResponseEntity(
            ExceptionMsg(
                resultCode = ResultCode.INVALID_PARAMETER.code,
                message = ResultCode.INVALID_PARAMETER.message,
            ),
            HttpStatus.BAD_REQUEST,
        )

    @ExceptionHandler(MethodArgumentTypeMismatchException::class)
    fun handleTypeMismatchHandler(ex: MethodArgumentTypeMismatchException): ResponseEntity<ExceptionMsg> =
        if (ex.requiredType?.isEnum == true) {
            ResponseEntity(
                ExceptionMsg(
                    resultCode = ResultCode.INVALID_PARAMETER.code,
                    message = ResultCode.INVALID_PARAMETER.message,
                ),
                HttpStatus.BAD_REQUEST,
            )
        } else {
            ResponseEntity(
                ExceptionMsg(
                    resultCode = ResultCode.INVALID_PARAMETER.code,
                    message = ResultCode.INVALID_PARAMETER.message,
                ),
                HttpStatus.BAD_REQUEST,
            )
        }
}
