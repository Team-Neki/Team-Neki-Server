package com.neki.admin

import com.neki.admin.common.infra.AdminWriteGuard
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.mock.web.MockFilterChain
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse

class AdminWriteGuardTest {
    @Test
    fun rejectsAdminWriteUntilAuthenticationIsAvailable() {
        val response = MockHttpServletResponse()
        val chain = MockFilterChain()

        AdminWriteGuard().doFilter(MockHttpServletRequest("POST", "/admin/v1/pose"), response, chain)

        assertEquals(403, response.status)
        assertEquals(null, chain.request)
    }

    @Test
    fun allowsAdminRead() {
        val response = MockHttpServletResponse()
        val chain = MockFilterChain()

        AdminWriteGuard().doFilter(MockHttpServletRequest("GET", "/admin/v1/pose"), response, chain)

        assertEquals(200, response.status)
        assertEquals("/admin/v1/pose", (chain.request as MockHttpServletRequest).requestURI)
    }
}
