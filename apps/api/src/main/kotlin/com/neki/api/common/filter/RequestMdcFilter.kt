package com.neki.api.common.filter

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.MDC
import org.springframework.web.filter.OncePerRequestFilter
import java.util.UUID

/**
 * fileName       : RequestMdcFilter
 * author         : koo
 * date           : 2025. 12. 31.
 * description    : 인증 전에 실행되어 요청별 기본 정보를 MDC에 설정하는 필터
 *                  Request ID, URI, Method, Client IP 등을 추가
 */
class RequestMdcFilter : OncePerRequestFilter() {

    companion object {
        const val REQUEST_ID = "requestId"
        const val REQUEST_URI = "requestUri"
        const val REQUEST_METHOD = "requestMethod"
        const val CLIENT_IP = "clientIp"
    }

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        try {
            // Request ID 설정 (헤더에서 가져오거나 새로 생성)
            val requestId: String = request.getHeader("X-Request-ID") ?: generateRequestId()
            MDC.put(REQUEST_ID, requestId)

            // 요청 정보 설정
            MDC.put(REQUEST_URI, request.requestURI)
            MDC.put(REQUEST_METHOD, request.method)
            MDC.put(CLIENT_IP, getClientIp(request))

            // Response 헤더에 Request ID 추가
            response.setHeader("X-Request-ID", requestId)

            filterChain.doFilter(request, response)
        } finally {
            // MDC 정리 (메모리 누수 방지)
            // 가장 먼저 실행되고 가장 마지막에 종료되므로 여기서 전체 MDC 정리
            MDC.clear()
        }
    }

    /**
     * 고유한 Request ID 생성
     */
    private fun generateRequestId(): String = UUID.randomUUID().toString().replace("-", "")

    /**
     * 클라이언트 IP 주소 추출
     * 프록시나 로드밸런서를 거치는 경우 X-Forwarded-For 헤더에서 실제 클라이언트 IP를 추출
     */
    private fun getClientIp(request: HttpServletRequest): String {
        val headers = listOf(
            "X-Forwarded-For",
            "Proxy-Client-IP",
            "WL-Proxy-Client-IP",
            "HTTP_X_FORWARDED_FOR",
            "HTTP_X_FORWARDED",
            "HTTP_X_CLUSTER_CLIENT_IP",
            "HTTP_CLIENT_IP",
            "HTTP_FORWARDED_FOR",
            "HTTP_FORWARDED",
            "HTTP_VIA",
            "REMOTE_ADDR",
        )

        for (header in headers) {
            val ip: String? = request.getHeader(header)
            if (!ip.isNullOrBlank() && ip != "unknown") {
                // X-Forwarded-For는 여러 IP가 콤마로 구분될 수 있음 (첫 번째가 실제 클라이언트 IP)
                return ip.split(",").firstOrNull()?.trim() ?: ip
            }
        }

        return request.remoteAddr ?: "unknown"
    }
}
