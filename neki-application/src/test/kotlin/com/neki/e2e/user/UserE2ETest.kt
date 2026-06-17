package com.neki.e2e.user

import com.neki.common.api.dto.ResultCode
import com.neki.e2e.E2ETestBase
import com.neki.user.entity.User
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
 * fileName       : UserE2ETest
 * author         : darren
 * date           : 2026. 01. 04.
 * description    : 사용자 API E2E 테스트
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class UserE2ETest : E2ETestBase() {

    @LocalServerPort
    private var port: Int = 0

    private val expiredAccessToken: String =
        "eyJhbGciOiJIUzM4NCJ9.eyJzdWIiOiIxIiwicm9sZXMiOlsiUk9MRV9VU0VSIl0sIm5hbWUiOiLthYzsiqTtirgg7IKs7Jqp7J6QIiwicHJvdmlkZXJfdHlwZSI6IlRFU1QiLCJpYXQiOjE3Njc1MTQyMjQsImV4cCI6MTc2NzUxNDI4NH0.QJ0T0eoYxMf7PUxQni2AGMMrNEMMFphY1W5vLE66vUyuPES-trmvqs7xbm9mp63v"
    private val expiredRefreshToken: String =
        "eyJhbGciOiJIUzM4NCJ9.eyJzdWIiOiIxIiwicm9sZXMiOlsiUk9MRV9VU0VSIl0sIm5hbWUiOiLthYzsiqTtirgg7IKs7Jqp7J6QIiwicHJvdmlkZXJfdHlwZSI6IlRFU1QiLCJpYXQiOjE3Njc1MTQyMjQsImV4cCI6MTc2NzUxNDI4NH0.KkvRf2fmXjr51Lk0Q8Xmd_MpKhJUY9m9WGZIqLH3yilMh47iv6Q7PIxmTUGuk1O1"

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
    @DisplayName("유효한 토큰으로 사용자 정보 조회 시 성공 응답을 반환한다")
    fun givenValidToken_whenGetUserInfo_thenReturnsSuccess() {
        RestAssured.given()
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer $accessToken")
            .`when`()
            .get("/api/users/info")
            .then()
            .statusCode(HttpStatus.OK.value())
            .body("resultCode", equalTo(ResultCode.SUCCESS.code))
            .body("data.name", equalTo(testUser.name))
            .body("data.providerType", equalTo(testUser.providerType.name))
    }

    @Test
    @DisplayName("만료된 Access Token으로 사용자 정보 조회 시 401 에러를 반환한다")
    fun givenExpiredToken_whenGetUserInfo_thenReturnsUnauthorized() {
        RestAssured.given()
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer $expiredAccessToken")
            .`when`()
            .get("/api/users/info")
            .then()
            .statusCode(HttpStatus.UNAUTHORIZED.value())
            .body("resultCode", equalTo(ResultCode.EXPIRED_TOKEN_ERROR.code))
    }

    @Test
    @DisplayName("토큰 없이 사용자 정보 조회 시 403 에러를 반환한다")
    fun givenNoToken_whenGetUserInfo_thenReturnsUnauthorized() {
        RestAssured.given()
            .contentType(ContentType.JSON)
            .`when`()
            .get("/api/users/info")
            .then()
            .statusCode(HttpStatus.FORBIDDEN.value())
    }

    @Test
    @DisplayName("잘못된 형식의 토큰으로 사용자 정보 조회 시 403 에러를 반환한다")
    fun givenInvalidToken_whenGetUserInfo_thenReturnsUnauthorized() {
        val invalidToken = "invalid.token.format"

        RestAssured.given()
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer $invalidToken")
            .`when`()
            .get("/api/users/info")
            .then()
            .statusCode(HttpStatus.FORBIDDEN.value())
            .body("resultCode", equalTo(ResultCode.INVALID_TOKEN_ERROR.code))
    }

    @Test
    @DisplayName("Bearer 접두사 없이 토큰으로 사용자 정보 조회 시 403 에러를 반환한다")
    fun givenTokenWithoutBearerPrefix_whenGetUserInfo_thenReturnsUnauthorized() {
        RestAssured.given()
            .contentType(ContentType.JSON)
            .header("Authorization", accessToken)
            .`when`()
            .get("/api/users/info")
            .then()
            .statusCode(HttpStatus.FORBIDDEN.value())
    }
}
