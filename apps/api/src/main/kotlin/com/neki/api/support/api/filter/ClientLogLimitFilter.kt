package com.neki.api.support.api.filter

import com.fasterxml.jackson.databind.ObjectMapper
import com.neki.core.code.ResultCode
import com.neki.core.exception.dto.ExceptionMsg
import jakarta.servlet.FilterChain
import jakarta.servlet.ReadListener
import jakarta.servlet.ServletInputStream
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletRequestWrapper
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import java.io.BufferedReader
import java.io.ByteArrayInputStream
import java.io.InputStreamReader
import java.util.concurrent.Semaphore

/**
 * fileName       : ClientLogLimitFilter
 * author         : koo
 * date           : 2026. 11. 3.
 * description    : 익명으로 열린 POST /api/logs 의 동시 처리 수와 본문 크기를 본문 역직렬화 전에 제한한다.
 *                  메모리 상한이 (동시 처리 수 x 본문 상한) 으로 정해지도록 permit 을 먼저 잡고 본문을 읽는다.
 *                  Firehose 한도(4MiB)의 정확한 판정은 서버 필드를 붙인 뒤 어댑터가 한다.
 */
@Component
class ClientLogLimitFilter(private val objectMapper: ObjectMapper) : OncePerRequestFilter() {

    private val log = LoggerFactory.getLogger(javaClass)

    companion object {
        // 어댑터의 4MiB 판정보다 여유를 둬서 정상 배치는 여기서 걸리지 않게 한다
        const val MAX_BODY_BYTES = 5 * 1024 * 1024

        // 로그 요청이 동시에 붙잡을 수 있는 Tomcat 스레드 수 (pod 당). Firehose 가 느려져 전송마다 타임아웃(5초)까지
        // 걸려도 기본 200개 중 나머지는 다른 API 몫으로 남긴다
        const val MAX_IN_FLIGHT = 20
    }

    private val inFlight = Semaphore(MAX_IN_FLIGHT)

    override fun shouldNotFilter(request: HttpServletRequest): Boolean =
        request.method != HttpMethod.POST.name() || request.requestURI != "/api/logs"

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        if (request.contentLengthLong > MAX_BODY_BYTES) {
            return reject(response, HttpStatus.PAYLOAD_TOO_LARGE, ResultCode.LOG_PAYLOAD_TOO_LARGE)
        }
        // 자리가 없으면 기다리지 않고 재시도 대상으로 끊는다. 클라이언트가 배치를 들고 있다가 나중에 다시 보낸다
        if (!inFlight.tryAcquire()) {
            log.warn("Client log request skipped: inFlight={}", MAX_IN_FLIGHT)
            return reject(response, HttpStatus.SERVICE_UNAVAILABLE, ResultCode.LOG_SEND_FAILED)
        }

        try {
            // Content-Length 가 없는 chunked 요청도 상한 + 1 바이트까지만 읽고 끊는다
            val body: ByteArray = request.inputStream.readNBytes(MAX_BODY_BYTES + 1)
            if (body.size > MAX_BODY_BYTES) {
                return reject(response, HttpStatus.PAYLOAD_TOO_LARGE, ResultCode.LOG_PAYLOAD_TOO_LARGE)
            }
            filterChain.doFilter(CachedBodyRequest(request, body), response)
        } finally {
            inFlight.release()
        }
    }

    private fun reject(response: HttpServletResponse, status: HttpStatus, resultCode: ResultCode) {
        response.status = status.value()
        response.contentType = MediaType.APPLICATION_JSON_VALUE
        response.characterEncoding = Charsets.UTF_8.name()
        objectMapper.writeValue(response.writer, ExceptionMsg(resultCode.code, resultCode.message))
    }

    private class CachedBodyRequest(request: HttpServletRequest, private val body: ByteArray) :
        HttpServletRequestWrapper(request) {

        override fun getInputStream(): ServletInputStream {
            val input = ByteArrayInputStream(body)
            return object : ServletInputStream() {
                override fun read(): Int = input.read()
                override fun read(b: ByteArray, off: Int, len: Int): Int = input.read(b, off, len)
                override fun isFinished(): Boolean = input.available() == 0
                override fun isReady(): Boolean = true
                override fun setReadListener(listener: ReadListener) = throw UnsupportedOperationException()
            }
        }

        override fun getReader(): BufferedReader =
            BufferedReader(InputStreamReader(inputStream, characterEncoding ?: Charsets.UTF_8.name()))
    }
}
