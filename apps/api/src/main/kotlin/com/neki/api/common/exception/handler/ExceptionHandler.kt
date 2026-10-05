package com.neki.api.common.exception.handler

import com.neki.core.code.ResultCode
import com.neki.core.exception.BusinessException
import com.neki.core.exception.dto.ExceptionMsg
import com.neki.core.exception.dto.FieldErrorDetail
import org.apache.catalina.connector.ClientAbortException
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.validation.FieldError
import org.springframework.validation.ObjectError
import org.springframework.web.HttpMediaTypeNotSupportedException
import org.springframework.web.HttpRequestMethodNotSupportedException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.MissingServletRequestParameterException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.context.request.WebRequest
import org.springframework.web.context.request.async.AsyncRequestNotUsableException
import org.springframework.web.method.annotation.HandlerMethodValidationException
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException
import org.springframework.web.servlet.NoHandlerFoundException
import org.springframework.web.servlet.resource.NoResourceFoundException
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

    @ExceptionHandler(ClientAbortException::class, AsyncRequestNotUsableException::class)
    fun handleClientAbort(ex: Exception) {
        // 클라이언트가 응답 수신 전에 연결을 끊음(브라우저 이탈, 네트워크 끊김, 요청 취소 등).
        // 서버 측 조치가 필요 없는 정상 현상이므로 스택트레이스 없이 debug 레벨로만 기록한다.
        // 이미 커밋/종료된 응답에 다시 write 하면 2차 예외가 발생하므로 아무것도 반환하지 않는다.
        log.debug("[CLIENT_ABORT] client closed connection before response was written: {}", ex.message)
    }

    @ExceptionHandler(NoResourceFoundException::class, NoHandlerFoundException::class)
    fun handleNotFound(ex: Exception): ResponseEntity<ExceptionMsg> {
        // 존재하지 않는 경로/정적 리소스 요청. 대부분 봇·스캐너의 경로 탐색이며 서버 측 조치가 필요 없다.
        // catch-all 핸들러가 잡으면 SYSTEM_ERROR ERROR 로그 + 400 응답이 나가 알림 노이즈가 되므로
        // 스택트레이스 없이 debug 레벨로만 기록하고 404를 반환한다.
        log.debug("[NOT_FOUND] no handler for request: {}", ex.message)

        return ResponseEntity(
            ExceptionMsg(
                resultCode = ResultCode.NOT_FOUND.code,
                message = ResultCode.NOT_FOUND.message,
            ),
            HttpStatus.NOT_FOUND,
        )
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException::class, HttpMediaTypeNotSupportedException::class)
    fun handleUnsupportedRequest(ex: Exception): ResponseEntity<ExceptionMsg> {
        // 허용되지 않은 HTTP 메서드/Content-Type 요청. 서버 결함이 아닌 클라이언트 오류이므로
        // debug 레벨로 기록하고 표준 상태코드를 반환한다.
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
