package com.yapp2app.common.filter

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import org.springframework.web.filter.OncePerRequestFilter

/**
 * API 요청의 수명주기(응답 시간, 상태 코드)를 로깅하는 필터.
 * RequestMdcFilter 이후에 실행되어 MDC 컨텍스트(requestId, userId)를 활용한다.
 *
 * JSON 로그: message는 간결하게, 상세 정보는 MDC 필드(durationMs, responseStatus, logCategory)로 분리.
 * 콘솔 로그: [API_REQUEST] POST /api/auth/kakao/login | status=200 | duration=342ms
 */
class RequestLoggingFilter : OncePerRequestFilter() {

    private val log = LoggerFactory.getLogger(javaClass)

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        val startTime = System.currentTimeMillis()

        try {
            filterChain.doFilter(request, response)
        } finally {
            val duration = System.currentTimeMillis() - startTime
            val status = response.status

            MDC.put("logCategory", "API_REQUEST")
            MDC.put("durationMs", duration.toString())
            MDC.put("responseStatus", status.toString())

            log.info("[API_REQUEST]")

            MDC.remove("logCategory")
            MDC.remove("durationMs")
            MDC.remove("responseStatus")
        }
    }

    override fun shouldNotFilter(request: HttpServletRequest): Boolean {
        val uri = request.requestURI
        return uri.startsWith("/actuator") ||
            uri.startsWith("/swagger-ui") ||
            uri.startsWith("/v3/api-docs") ||
            uri.startsWith("/file")
    }
}
