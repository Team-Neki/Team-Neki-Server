package com.yapp2app.e2e.auth

import com.yapp2app.auth.api.dto.LoginRequest
import com.yapp2app.auth.api.dto.RefreshTokenRequest
import com.yapp2app.common.api.dto.ResultCode
import com.yapp2app.e2e.E2ETestBase
import com.yapp2app.user.domain.entity.User
import com.yapp2app.user.domain.enums.ProviderType
import io.restassured.RestAssured
import io.restassured.http.ContentType
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.CoreMatchers.notNullValue
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

    private val expiredAccessToken: String = "eyJhbGciOiJIUzM4NCJ9.eyJzdWIiOiIxIiwicm9sZXMiOlsiUk9MRV9VU0VSIl0sIm5hbWUiOiLthYzsiqTtirgg7IKs7Jqp7J6QIiwicHJvdmlkZXJfdHlwZSI6IlRFU1QiLCJpYXQiOjE3Njc1MTQyMjQsImV4cCI6MTc2NzUxNDI4NH0.QJ0T0eoYxMf7PUxQni2AGMMrNEMMFphY1W5vLE66vUyuPES-trmvqs7xbm9mp63v"
    private val expiredRefreshToken: String = "eyJhbGciOiJIUzM4NCJ9.eyJzdWIiOiIxIiwicm9sZXMiOlsiUk9MRV9VU0VSIl0sIm5hbWUiOiLthYzsiqTtirgg7IKs7Jqp7J6QIiwicHJvdmlkZXJfdHlwZSI6IlRFU1QiLCJpYXQiOjE3Njc1MTQyMjQsImV4cCI6MTc2NzUxNDI4NH0.KkvRf2fmXjr51Lk0Q8Xmd_MpKhJUY9m9WGZIqLH3yilMh47iv6Q7PIxmTUGuk1O1"

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
    @DisplayName("유효한 사용자 정보로 로그인 요청 시 성공 응답과 토큰을 반환한다")
    fun givenValidCredentials_whenLogin_thenReturnsSuccessWithTokens() {
        val request = LoginRequest(
            oid = testUser.oid,
            providerType = testUser.providerType
        )

        val response = RestAssured.given()
            .contentType(ContentType.JSON)
            .body(request)
            .`when`()
            .post("/api/auth/login")
            .then()
            .statusCode(HttpStatus.OK.value())
            .body("success", equalTo(true))
            .body("resultCode", equalTo(ResultCode.SUCCESS.code))
            .body("data.accessToken", notNullValue())
            .body("data.refreshToken", notNullValue())
            .extract()
            .response()

        val accessToken = response.jsonPath().getString("data.accessToken")
        val refreshToken = response.jsonPath().getString("data.refreshToken")

        println("========================================")
        println("🔑 Access Token: $accessToken")
        println("🔄 Refresh Token: $refreshToken")
        println("========================================")
    }

    @Test
    @DisplayName("존재하지 않는 사용자로 로그인 요청 시 400 에러를 반환한다")
    fun givenNonExistentUser_whenLogin_thenReturnsNotFoundError() {
        val request = LoginRequest(
            oid = 99999L,
            providerType = ProviderType.TEST
        )

        RestAssured.given()
            .contentType(ContentType.JSON)
            .body(request)
            .`when`()
            .post("/api/auth/login")
            .then()
            .statusCode(HttpStatus.BAD_REQUEST.value())
            .body("success", equalTo(false))
            .body("resultCode", equalTo(ResultCode.NOT_FOUND_USER.code))
    }

    @Test
    @DisplayName("유효한 Refresh Token으로 토큰 갱신 요청 시 새로운 토큰을 반환한다")
    fun givenValidRefreshToken_whenRefresh_thenReturnsNewTokens() {
        // 먼저 로그인하여 토큰 획득
        val loginRequest = LoginRequest(
            oid = testUser.oid,
            providerType = testUser.providerType
        )

        val loginResponse = RestAssured.given()
            .contentType(ContentType.JSON)
            .body(loginRequest)
            .`when`()
            .post("/api/auth/login")
            .then()
            .statusCode(HttpStatus.OK.value())
            .extract()
            .jsonPath()

        val refreshToken = loginResponse.getString("data.refreshToken")

        // Refresh Token으로 토큰 갱신
        val refreshRequest = RefreshTokenRequest(refreshToken = refreshToken)

        RestAssured.given()
            .contentType(ContentType.JSON)
            .body(refreshRequest)
            .`when`()
            .post("/api/auth/refresh")
            .then()
            .statusCode(HttpStatus.OK.value())
            .body("success", equalTo(true))
            .body("resultCode", equalTo(ResultCode.SUCCESS.code))
            .body("data.accessToken", notNullValue())
            .body("data.refreshToken", notNullValue())
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
            .body("resultCode", equalTo(ResultCode.INVALID_TOKEN_ERROR.code))
    }

}