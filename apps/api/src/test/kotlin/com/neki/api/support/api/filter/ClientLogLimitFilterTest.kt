package com.neki.api.support.api.filter

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.neki.core.code.ResultCode
import jakarta.servlet.FilterChain
import jakarta.servlet.ServletRequest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import java.util.concurrent.Callable
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

@DisplayName("ClientLogLimitFilter")
class ClientLogLimitFilterTest {

    private val objectMapper = jacksonObjectMapper()
    private val filter = ClientLogLimitFilter(objectMapper)

    private fun request(body: ByteArray, withContentLength: Boolean = true): MockHttpServletRequest {
        val request = if (withContentLength) {
            MockHttpServletRequest()
        } else {
            // chunked 요청처럼 Content-Length 가 없는 경우
            object : MockHttpServletRequest() {
                override fun getContentLengthLong(): Long = -1
                override fun getContentLength(): Int = -1
            }
        }
        request.method = "POST"
        request.requestURI = "/api/logs"
        request.setContent(body)
        return request
    }

    private fun resultCode(response: MockHttpServletResponse): Any? =
        objectMapper.readValue(response.contentAsString, Map::class.java)["resultCode"]

    @Test
    @DisplayName("상한 이내 본문은 그대로 다시 읽을 수 있게 다음 필터로 넘긴다")
    fun passesBodyThrough() {
        var received: String? = null

        filter.doFilter(
            request("""{"platform":"IOS"}""".toByteArray()),
            MockHttpServletResponse(),
            FilterChain { req, _ -> received = req.inputStream.readAllBytes().decodeToString() },
        )

        assertEquals("""{"platform":"IOS"}""", received)
    }

    @Test
    @DisplayName("Content-Length 가 상한을 넘으면 본문을 읽지 않고 413 D-16 을 반환한다")
    fun rejectsDeclaredOversizedBody() {
        val response = MockHttpServletResponse()
        var called = false

        filter.doFilter(
            request(ByteArray(ClientLogLimitFilter.MAX_BODY_BYTES + 1)),
            response,
            FilterChain { _, _ -> called = true },
        )

        assertEquals(413, response.status)
        assertEquals(ResultCode.LOG_PAYLOAD_TOO_LARGE.code, resultCode(response))
        assertTrue(!called)
    }

    @Test
    @DisplayName("Content-Length 없는 본문도 읽다가 상한을 넘으면 413 D-16 을 반환한다")
    fun rejectsChunkedOversizedBody() {
        val response = MockHttpServletResponse()
        var forwarded: ServletRequest? = null

        filter.doFilter(
            request(ByteArray(ClientLogLimitFilter.MAX_BODY_BYTES + 1), withContentLength = false),
            response,
            FilterChain { req, _ -> forwarded = req },
        )

        assertEquals(413, response.status)
        assertEquals(ResultCode.LOG_PAYLOAD_TOO_LARGE.code, resultCode(response))
        assertNull(forwarded)
    }

    @Test
    @DisplayName("동시 처리가 상한(20)에 차면 본문을 읽지 않고 503 D-15 를 반환하고, 처리가 끝나면 자리를 돌려준다")
    fun rejectsWhenTooManyInFlight() {
        val maxInFlight = ClientLogLimitFilter.MAX_IN_FLIGHT
        val entered = CountDownLatch(maxInFlight)
        val release = CountDownLatch(1)
        val blocking = FilterChain { _, _ ->
            entered.countDown()
            release.await(5, TimeUnit.SECONDS)
        }
        val pool = Executors.newFixedThreadPool(maxInFlight)

        try {
            val running = List(maxInFlight) {
                pool.submit(Callable { filter.doFilter(request(ByteArray(1)), MockHttpServletResponse(), blocking) })
            }
            assertTrue(entered.await(5, TimeUnit.SECONDS))

            val rejected = MockHttpServletResponse()
            filter.doFilter(request(ByteArray(1)), rejected, FilterChain { _, _ -> error("should not be called") })
            assertEquals(503, rejected.status)
            assertEquals(ResultCode.LOG_SEND_FAILED.code, resultCode(rejected))

            release.countDown()
            running.forEach { it.get(5, TimeUnit.SECONDS) }
        } finally {
            release.countDown()
            pool.shutdownNow()
        }

        var called = false
        filter.doFilter(request(ByteArray(1)), MockHttpServletResponse(), FilterChain { _, _ -> called = true })
        assertTrue(called)
    }
}
