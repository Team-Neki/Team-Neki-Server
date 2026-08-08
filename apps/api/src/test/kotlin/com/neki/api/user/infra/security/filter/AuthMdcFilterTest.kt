package com.neki.api.user.infra.security.filter

import com.neki.api.common.filter.RequestMdcFilter
import com.neki.api.user.infra.security.token.UserPrincipal
import com.neki.domain.user.models.ProviderType
import jakarta.servlet.FilterChain
import jakarta.servlet.ServletRequest
import jakarta.servlet.ServletResponse
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.slf4j.MDC
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
    @DisplayName("인증된 UserPrincipal의 userId가 MDC에 설정된다")
    fun givenAuthenticatedUserPrincipal_whenFilterExecutes_thenSetsUserIdInMdc() {
        // Given
        val principal = createUserPrincipal(id = 12345L)
        SecurityContextHolder.getContext().authentication = authenticatedAuth(principal)

        val filterChain = TestFilterChain()

        // When
        filter.doFilter(MockHttpServletRequest(), MockHttpServletResponse(), filterChain)

        // Then
        Assertions.assertThat(filterChain.capturedUserId).isEqualTo("12345")
    }

    @Test
    @DisplayName("principal이 UserPrincipal이 아니면 MDC에 userId를 설정하지 않는다")
    fun givenStringPrincipal_whenFilterExecutes_thenDoesNotSetUserId() {
        // Given
        SecurityContextHolder.getContext().authentication =
            authenticatedAuth("12345")

        val filterChain = TestFilterChain()

        // When
        filter.doFilter(MockHttpServletRequest(), MockHttpServletResponse(), filterChain)

        // Then
        Assertions.assertThat(filterChain.capturedUserId).isNull()
    }

    @Test
    @DisplayName("Authentication이 없으면 MDC에 userId를 설정하지 않는다")
    fun givenNoAuthentication_whenFilterExecutes_thenDoesNotSetUserId() {
        val filterChain = TestFilterChain()

        filter.doFilter(MockHttpServletRequest(), MockHttpServletResponse(), filterChain)

        Assertions.assertThat(filterChain.capturedUserId).isNull()
    }

    @Test
    @DisplayName("기존 MDC 값(requestId)을 유지하면서 userId를 추가한다")
    fun givenExistingMdc_whenFilterExecutes_thenPreservesValues() {
        // Given
        MDC.put(RequestMdcFilter.REQUEST_ID, "req-123")

        val principal = createUserPrincipal(id = 99L)
        SecurityContextHolder.getContext().authentication = authenticatedAuth(principal)

        val filterChain = TestFilterChain()

        // When
        filter.doFilter(MockHttpServletRequest(), MockHttpServletResponse(), filterChain)

        // Then
        Assertions.assertThat(filterChain.capturedRequestId).isEqualTo("req-123")
        Assertions.assertThat(filterChain.capturedUserId).isEqualTo("99")
    }

    @Test
    @DisplayName("다른 필터에서 설정한 MDC 값을 유지하면서 userId를 추가한다")
    fun givenExistingMdcValues_whenFilterExecutes_thenPreservesExistingValues() {
        // Given
        val expectedRequestId = "request-id-12345"
        MDC.put(RequestMdcFilter.REQUEST_ID, expectedRequestId)

        val principal = createUserPrincipal(id = 12345L)
        SecurityContextHolder.getContext().authentication =
            authenticatedAuth(principal)

        val filterChain = TestFilterChain()

        // When
        filter.doFilter(MockHttpServletRequest(), MockHttpServletResponse(), filterChain)

        // Then
        Assertions.assertThat(filterChain.capturedRequestId).isEqualTo(expectedRequestId)
        Assertions.assertThat(filterChain.capturedUserId).isEqualTo("12345")
    }

    /**
     * 테스트용 Authentication 객체 생성
     */
    private fun createUserPrincipal(id: Long = 1L): UserPrincipal = UserPrincipal(
        id = id,
        name = "test-user",
        providerType = ProviderType.LOCAL,
        email = "test@test.com",
        roles = setOf("ROLE_USER"),
        password = "NO_PASS",
    )

    private fun authenticatedAuth(principal: Any): UsernamePasswordAuthenticationToken =
        UsernamePasswordAuthenticationToken(
            principal,
            null,
            listOf(SimpleGrantedAuthority("ROLE_USER")),
        )

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
