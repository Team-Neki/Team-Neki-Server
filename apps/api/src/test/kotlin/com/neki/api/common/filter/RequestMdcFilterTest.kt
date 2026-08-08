package com.neki.api.common.filter

import jakarta.servlet.FilterChain
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.slf4j.MDC
import org.springframework.mock.web.MockFilterChain
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse

@DisplayName("RequestMdcFilter 테스트")
class RequestMdcFilterTest {

    private val filter = RequestMdcFilter()

    @AfterEach
    fun tearDown() {
        // 각 테스트 후 MDC 정리
        MDC.clear()
    }

    @Test
    @DisplayName("Request ID가 헤더에 없을 때 자동으로 생성한다")
    fun givenNoRequestIdHeader_whenFilterExecutes_thenGeneratesRequestId() {
        // Given
        val request = MockHttpServletRequest()
        val response = MockHttpServletResponse()
        val filterChain = TestFilterChain()

        // When
        filter.doFilter(request, response, filterChain)

        // Then
        val requestId = filterChain.capturedRequestId
        assertThat(requestId).isNotNull()
        assertThat(requestId).hasSize(32) // UUID without hyphens
        assertThat(response.getHeader("X-Request-ID")).isEqualTo(requestId)
    }

    @Test
    @DisplayName("Request ID가 헤더에 있을 때 해당 값을 사용한다")
    fun givenRequestIdInHeader_whenFilterExecutes_thenUsesHeaderValue() {
        // Given
        val expectedRequestId = "test-request-id-12345"
        val request = MockHttpServletRequest().apply {
            addHeader("X-Request-ID", expectedRequestId)
        }
        val response = MockHttpServletResponse()
        val filterChain = TestFilterChain()

        // When
        filter.doFilter(request, response, filterChain)

        // Then
        val actualRequestId = filterChain.capturedRequestId
        assertThat(actualRequestId).isEqualTo(expectedRequestId)
        assertThat(response.getHeader("X-Request-ID")).isEqualTo(expectedRequestId)
    }

    @Test
    @DisplayName("요청 URI가 MDC에 설정된다")
    fun givenRequest_whenFilterExecutes_thenSetsRequestUriInMdc() {
        // Given
        val expectedUri = "/api/users/123"
        val request = MockHttpServletRequest().apply {
            requestURI = expectedUri
        }
        val response = MockHttpServletResponse()
        val filterChain = TestFilterChain()

        // When
        filter.doFilter(request, response, filterChain)

        // Then
        assertThat(filterChain.capturedRequestUri).isEqualTo(expectedUri)
    }

    @Test
    @DisplayName("요청 메소드가 MDC에 설정된다")
    fun givenRequest_whenFilterExecutes_thenSetsRequestMethodInMdc() {
        // Given
        val expectedMethod = "POST"
        val request = MockHttpServletRequest().apply {
            method = expectedMethod
        }
        val response = MockHttpServletResponse()
        val filterChain = TestFilterChain()

        // When
        filter.doFilter(request, response, filterChain)

        // Then
        assertThat(filterChain.capturedRequestMethod).isEqualTo(expectedMethod)
    }

    @Test
    @DisplayName("클라이언트 IP가 MDC에 설정된다")
    fun givenRequest_whenFilterExecutes_thenSetsClientIpInMdc() {
        // Given
        val expectedIp = "192.168.1.100"
        val request = MockHttpServletRequest().apply {
            remoteAddr = expectedIp
        }
        val response = MockHttpServletResponse()
        val filterChain = TestFilterChain()

        // When
        filter.doFilter(request, response, filterChain)

        // Then
        assertThat(filterChain.capturedClientIp).isEqualTo(expectedIp)
    }

    @Test
    @DisplayName("X-Forwarded-For 헤더가 있을 때 실제 클라이언트 IP를 추출한다")
    fun givenXForwardedForHeader_whenFilterExecutes_thenExtractsRealClientIp() {
        // Given
        val realClientIp = "203.0.113.195"
        val proxyIp = "192.168.1.1"
        val request = MockHttpServletRequest().apply {
            addHeader("X-Forwarded-For", "$realClientIp, $proxyIp")
            remoteAddr = proxyIp
        }
        val response = MockHttpServletResponse()
        val filterChain = TestFilterChain()

        // When
        filter.doFilter(request, response, filterChain)

        // Then
        assertThat(filterChain.capturedClientIp).isEqualTo(realClientIp)
    }

    @Test
    @DisplayName("필터 체인 실행 후 MDC가 정리된다")
    fun givenFilterExecution_whenFilterCompletes_thenClearsMdc() {
        // Given
        val request = MockHttpServletRequest()
        val response = MockHttpServletResponse()
        val filterChain = MockFilterChain()

        // When
        filter.doFilter(request, response, filterChain)

        // Then - MDC가 비어있어야 함
        assertThat(MDC.get(RequestMdcFilter.REQUEST_ID)).isNull()
        assertThat(MDC.get(RequestMdcFilter.REQUEST_URI)).isNull()
        assertThat(MDC.get(RequestMdcFilter.REQUEST_METHOD)).isNull()
        assertThat(MDC.get(RequestMdcFilter.CLIENT_IP)).isNull()
    }

    @Test
    @DisplayName("Response 헤더에 Request ID가 추가된다")
    fun givenRequest_whenFilterExecutes_thenAddsRequestIdToResponseHeader() {
        // Given
        val request = MockHttpServletRequest()
        val response = MockHttpServletResponse()
        val filterChain = MockFilterChain()

        // When
        filter.doFilter(request, response, filterChain)

        // Then
        assertThat(response.getHeader("X-Request-ID")).isNotNull()
    }

    /**
     * FilterChain 실행 중 MDC 값을 캡처하는 테스트용 FilterChain
     */
    private class TestFilterChain : FilterChain {
        var capturedRequestId: String? = null
        var capturedRequestUri: String? = null
        var capturedRequestMethod: String? = null
        var capturedClientIp: String? = null

        override fun doFilter(request: jakarta.servlet.ServletRequest?, response: jakarta.servlet.ServletResponse?) {
            // FilterChain 실행 시점에 MDC 값 캡처
            capturedRequestId = MDC.get(RequestMdcFilter.REQUEST_ID)
            capturedRequestUri = MDC.get(RequestMdcFilter.REQUEST_URI)
            capturedRequestMethod = MDC.get(RequestMdcFilter.REQUEST_METHOD)
            capturedClientIp = MDC.get(RequestMdcFilter.CLIENT_IP)
        }
    }
}
