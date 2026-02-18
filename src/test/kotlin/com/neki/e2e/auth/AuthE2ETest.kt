package com.neki.e2e.auth

import com.neki.e2e.E2ETestBase
import com.neki.user.domain.entity.User
import io.restassured.RestAssured
import org.junit.jupiter.api.BeforeEach
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.test.context.ActiveProfiles

/**
 * fileName       : AuthE2ETest
 * author         : darren
 * date           : 2026. 01. 04.
 * description    : 인증/인가 E2E 테스트
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class AuthE2ETest : E2ETestBase() {

    @LocalServerPort
    private var port: Int = 0

    private lateinit var accessToken: String
    private lateinit var testUser: User

    @BeforeEach
    fun setUp() {
        RestAssured.port = port
        RestAssured.baseURI = "http://localhost"

        // 테스트 사용자 생성
        val (user, token) = createTestUserAndToken()
        testUser = user
        accessToken = token
    }
}
