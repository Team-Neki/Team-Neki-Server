package com.yapp2app.e2e.auth

import com.yapp2app.auth.api.dto.RefreshTokenRequest
import com.yapp2app.common.api.dto.ResultCode
import com.yapp2app.e2e.E2ETestBase
import com.yapp2app.user.domain.entity.User
import io.restassured.RestAssured
import io.restassured.http.ContentType
import org.hamcrest.CoreMatchers.equalTo
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.http.HttpStatus
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


    @Test
    @DisplayName("유효하지 않은 Refresh Token으로 토큰 갱신 요청 시 400 에러를 반환한다")
    fun givenInvalidRefreshToken_whenRefresh_thenReturnsInvalidTokenError() {
        val invalidRefreshToken = "invalid.refresh.token"
        val request = RefreshTokenRequest(refreshToken = invalidRefreshToken)

        RestAssured.given()
            .contentType(ContentType.JSON)
            .body(request)
            .`when`()
            .post("/api/auth/refresh")
            .then()
            .statusCode(HttpStatus.BAD_REQUEST.value())
            .body("success", equalTo(false))
            .body("resultCode", equalTo(ResultCode.INVALID_TOKEN_ERROR.code))
    }

    @Test
    @DisplayName("빈 Refresh Token으로 토큰 갱신 요청 시 400 에러를 반환한다")
    fun givenBlankRefreshToken_whenRefresh_thenReturnsBadRequest() {
        val request = RefreshTokenRequest(refreshToken = "")

        RestAssured.given()
            .contentType(ContentType.JSON)
            .body(request)
            .`when`()
            .post("/api/auth/refresh")
            .then()
            .statusCode(HttpStatus.BAD_REQUEST.value())
            .body("success", equalTo(false))
            .body("resultCode", equalTo(ResultCode.INVALID_PARAMETER.code))
    }
}
