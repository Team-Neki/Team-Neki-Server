package com.neki.api.e2e.common

import com.neki.api.e2e.E2ETestBase
import com.neki.core.code.ResultCode
import io.restassured.RestAssured
import org.hamcrest.Matchers.equalTo
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.http.HttpStatus
import org.springframework.test.context.ActiveProfiles

/**
 * 봇/스캐너의 경로 탐색이 [SYSTEM_ERROR] 로그로 기록되지 않도록,
 * 404성 예외가 catch-all 핸들러가 아닌 전용 핸들러로 처리되는지 검증한다.
 */
@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class NotFoundExceptionE2ETest : E2ETestBase() {

    @LocalServerPort
    private var port: Int = 0

    @BeforeEach
    fun setUp() {
        RestAssured.port = port
        RestAssured.baseURI = "http://localhost"
    }

    @Test
    @DisplayName("존재하지 않는 정적 리소스 요청 시 400이 아닌 404를 반환한다")
    fun givenUnknownStaticResource_whenGet_thenReturnsNotFound() {
        RestAssured.given()
            .`when`()
            .get("/swagger-ui/index.html./swagger-ui-standalone-preset.js")
            .then()
            .statusCode(HttpStatus.NOT_FOUND.value())
            .body("resultCode", equalTo(ResultCode.NOT_FOUND.code))
    }

    @Test
    @DisplayName("지원하지 않는 HTTP 메서드 요청 시 405를 반환한다")
    fun givenUnsupportedMethod_whenRequest_thenReturnsMethodNotAllowed() {
        RestAssured.given()
            .`when`()
            .delete("/api/terms")
            .then()
            .statusCode(HttpStatus.METHOD_NOT_ALLOWED.value())
            .body("resultCode", equalTo(ResultCode.INVALID_PARAMETER.code))
    }
}
