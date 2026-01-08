package com.yapp2app.common.exception.handler

import com.fasterxml.jackson.databind.JsonMappingException
import com.fasterxml.jackson.databind.exc.InvalidFormatException
import com.yapp2app.common.api.dto.ResultCode
import com.yapp2app.common.exception.BusinessException
import com.yapp2app.common.exception.dto.ExceptionMsg
import com.yapp2app.common.exception.dto.FieldErrorDetail
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.validation.FieldError
import org.springframework.validation.ObjectError
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.MissingServletRequestParameterException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.context.request.WebRequest
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

    @ExceptionHandler(BusinessException::class)
    fun businessExceptionHandler(ex: BusinessException): ResponseEntity<ExceptionMsg> {
        val temp = ResponseEntity(
            ExceptionMsg(
                resultCode = ex.resultCode.code,
                message = ex.resultCode.message,
                success = false,
                errors = emptyList(),
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
                success = false,
                errors = errors,
            ),
            HttpStatus.BAD_REQUEST,
        )
    }

    @ExceptionHandler(HttpMessageNotReadableException::class)
    fun handleHttpMessageNotReadableExceptionHandler(
        ex: HttpMessageNotReadableException,
        request: WebRequest,
    ): ResponseEntity<ExceptionMsg> {
        val rootCause = ex.cause

        val errors: MutableList<FieldErrorDetail> = ArrayList<FieldErrorDetail>()
        if (rootCause is InvalidFormatException) {
            for (reference in rootCause.path) {
                errors.add(
                    FieldErrorDetail(
                        field = reference.fieldName,
                        message = "[" + reference.fieldName + "] 가 타입이 알맞지 않습니다.",
                    ),
                )
            }
        } else if (rootCause is JsonMappingException) {
            for (reference in rootCause.path) {
                errors.add(
                    FieldErrorDetail(
                        field = reference.fieldName,
                        message = "[" + reference.fieldName + "] 가 누락되었습니다.",
                    ),
                )
            }
        } else {
            errors.add(
                FieldErrorDetail(
                    field = "",
                    message = rootCause?.message ?: "에러 발생",
                ),
            )
        }
        return ResponseEntity(
            ExceptionMsg(
                resultCode = ResultCode.INVALID_PARAMETER.code,
                message = ResultCode.INVALID_PARAMETER.message,
                success = false,
                errors = errors,
            ),
            HttpStatus.BAD_REQUEST,
        )
    }

    @ExceptionHandler(MissingServletRequestParameterException::class)
    fun handleMissingServletRequestParameterExceptionHandler(
        ex: MissingServletRequestParameterException,
        request: WebRequest,
    ): ResponseEntity<ExceptionMsg> = ResponseEntity(
        ExceptionMsg(
            resultCode = ResultCode.INVALID_PARAMETER.code,
            message = ResultCode.INVALID_PARAMETER.message,
            success = false,
            errors = listOf(
                FieldErrorDetail(
                    field = ex.parameterName,
                    message = ex.message,
                ),
            ),
        ),
        HttpStatus.BAD_REQUEST,
    )

    @ExceptionHandler(MethodArgumentTypeMismatchException::class)
    @ResponseStatus(HttpStatus.OK)
    fun handleTypeMismatchHandler(ex: MethodArgumentTypeMismatchException): ResponseEntity<ExceptionMsg> =
        if (ex.requiredType?.isEnum == true) {
            val enumValues = ex.requiredType!!.enumConstants?.joinToString(", ")
            ResponseEntity(
                ExceptionMsg(
                    resultCode = ResultCode.INVALID_PARAMETER.code,
                    message = ResultCode.INVALID_PARAMETER.message,
                    success = false,
                    errors = listOf(
                        FieldErrorDetail(
                            field = ex.name,
                            message = enumValues.toString(),
                        ),
                    ),
                ),
                HttpStatus.BAD_REQUEST,
            )
        } else {
            ResponseEntity(
                ExceptionMsg(
                    resultCode = ResultCode.INVALID_PARAMETER.code,
                    message = ResultCode.INVALID_PARAMETER.message,
                    success = false,
                    errors = listOf(
                        FieldErrorDetail(
                            field = ex.name,
                            message = "Invalid value for parameter '${ex.name}'",
                        ),
                    ),
                ),
                HttpStatus.BAD_REQUEST,
            )
        }
}
