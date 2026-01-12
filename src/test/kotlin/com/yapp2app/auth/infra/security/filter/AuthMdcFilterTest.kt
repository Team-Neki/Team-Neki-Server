package com.yapp2app.auth.infra.security.filter

import com.yapp2app.common.filter.RequestMdcFilter
import jakarta.servlet.FilterChain
import jakarta.servlet.ServletRequest
import jakarta.servlet.ServletResponse
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.slf4j.MDC
import org.springframework.mock.web.MockFilterChain
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder

@DisplayName("AuthMdcFilter 테스트")
class AuthMdcFilterTest {

    private val filter = AuthMdcFilter()

    @AfterEach
    fun tearDown() {
        // 각 테스트 후 MDC 및 SecurityContext 정리
        MDC.clear()
        SecurityContextHolder.clearContext()
    }

    @Test
    @DisplayName("인증된 사용자의 userId가 MDC에 설정된다")
    fun givenAuthenticatedUser_whenFilterExecutes_thenSetsUserIdInMdc() {
        // Given
        val expectedUserId = "user-12345"
        val authentication = createAuthentication(expectedUserId, authenticated = true)
        SecurityContextHolder.getContext().authentication = authentication

        val request = MockHttpServletRequest()
        val response = MockHttpServletResponse()
        val filterChain = TestFilterChain()

        // When
        filter.doFilter(request, response, filterChain)

        // Then
        Assertions.assertThat(filterChain.capturedUserId).isEqualTo(expectedUserId)
    }

    @Test
    @DisplayName("익명 사용자일 때 userId가 MDC에 설정되지 않는다")
    fun givenAnonymousUser_whenFilterExecutes_thenDoesNotSetUserId() {
        // Given
        val authentication = createAuthentication("anonymousUser", authenticated = true)
        SecurityContextHolder.getContext().authentication = authentication

        val request = MockHttpServletRequest()
        val response = MockHttpServletResponse()
        val filterChain = TestFilterChain()

        // When
        filter.doFilter(request, response, filterChain)

        // Then
        Assertions.assertThat(filterChain.capturedUserId).isNull()
    }

    @Test
    @DisplayName("인증되지 않은 사용자일 때 userId가 MDC에 설정되지 않는다")
    fun givenUnauthenticatedUser_whenFilterExecutes_thenDoesNotSetUserId() {
        // Given
        val authentication = createAuthentication("user-12345", authenticated = false)
        SecurityContextHolder.getContext().authentication = authentication

        val request = MockHttpServletRequest()
        val response = MockHttpServletResponse()
        val filterChain = TestFilterChain()

        // When
        filter.doFilter(request, response, filterChain)

        // Then
        Assertions.assertThat(filterChain.capturedUserId).isNull()
    }

    @Test
    @DisplayName("인증 정보가 없을 때 userId가 MDC에 설정되지 않는다")
    fun givenNoAuthentication_whenFilterExecutes_thenDoesNotSetUserId() {
        // Given - SecurityContext에 인증 정보 없음
        val request = MockHttpServletRequest()
        val response = MockHttpServletResponse()
        val filterChain = TestFilterChain()

        // When
        filter.doFilter(request, response, filterChain)

        // Then
        Assertions.assertThat(filterChain.capturedUserId).isNull()
    }

    @Test
    @DisplayName("필터 체인이 정상적으로 실행된다")
    fun givenRequest_whenFilterExecutes_thenContinuesFilterChain() {
        // Given
        val authentication = createAuthentication("user-12345", authenticated = true)
        SecurityContextHolder.getContext().authentication = authentication

        val request = MockHttpServletRequest()
        val response = MockHttpServletResponse()
        val filterChain = MockFilterChain()

        // When
        filter.doFilter(request, response, filterChain)

        // Then - 필터 체인이 실행되었는지 확인
        Assertions.assertThat(filterChain.request).isEqualTo(request)
        Assertions.assertThat(filterChain.response).isEqualTo(response)
    }

    @Test
    @DisplayName("권한이 있는 인증된 사용자의 userId가 MDC에 설정된다")
    fun givenAuthenticatedUserWithAuthorities_whenFilterExecutes_thenSetsUserIdInMdc() {
        // Given
        val expectedUserId = "admin-99999"
        val authorities = listOf(
            SimpleGrantedAuthority("ROLE_ADMIN"),
            SimpleGrantedAuthority("ROLE_USER"),
        )
        val authentication = UsernamePasswordAuthenticationToken(
            expectedUserId,
            "password",
            authorities,
        )
        SecurityContextHolder.getContext().authentication = authentication

        val request = MockHttpServletRequest()
        val response = MockHttpServletResponse()
        val filterChain = TestFilterChain()

        // When
        filter.doFilter(request, response, filterChain)

        // Then
        Assertions.assertThat(filterChain.capturedUserId).isEqualTo(expectedUserId)
    }

    @Test
    @DisplayName("다른 필터에서 설정한 MDC 값을 유지한다")
    fun givenExistingMdcValues_whenFilterExecutes_thenPreservesExistingValues() {
        // Given
        val expectedRequestId = "request-id-12345"
        MDC.put(RequestMdcFilter.Companion.REQUEST_ID, expectedRequestId)

        val expectedUserId = "user-12345"
        val authentication = createAuthentication(expectedUserId, authenticated = true)
        SecurityContextHolder.getContext().authentication = authentication

        val request = MockHttpServletRequest()
        val response = MockHttpServletResponse()
        val filterChain = TestFilterChain()

        // When
        filter.doFilter(request, response, filterChain)

        // Then
        Assertions.assertThat(filterChain.capturedRequestId).isEqualTo(expectedRequestId)
        Assertions.assertThat(filterChain.capturedUserId).isEqualTo(expectedUserId)
    }

    /**
     * 테스트용 Authentication 객체 생성
     */
    private fun createAuthentication(username: String, authenticated: Boolean): UsernamePasswordAuthenticationToken =
        if (authenticated) {
            // authenticated = true: authorities를 포함하는 생성자 사용
            UsernamePasswordAuthenticationToken(username, "password", emptyList())
        } else {
            // authenticated = false: authorities를 포함하지 않는 생성자 사용
            UsernamePasswordAuthenticationToken(username, "password")
        }

    /**
     * FilterChain 실행 중 MDC 값을 캡처하는 테스트용 FilterChain
     */
    private class TestFilterChain : FilterChain {
        var capturedUserId: String? = null
        var capturedRequestId: String? = null

        override fun doFilter(request: ServletRequest?, response: ServletResponse?) {
            // FilterChain 실행 시점에 MDC 값 캡처
            capturedUserId = MDC.get(AuthMdcFilter.USER_ID)
            capturedRequestId = MDC.get(RequestMdcFilter.Companion.REQUEST_ID)
        }
    }
}
