package com.neki.api.e2e.media

import com.neki.core.code.ResultCode
import com.neki.domain.user.models.User
import io.restassured.RestAssured
import org.assertj.core.api.Assertions.assertThat
import org.hamcrest.Matchers.equalTo
import org.hamcrest.Matchers.matchesPattern
import org.hamcrest.Matchers.notNullValue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.http.HttpStatus
import org.springframework.test.context.ActiveProfiles
import java.time.LocalDate
import java.time.ZoneId

/**
 * fileName       : GenerateQrDumpUploadTicketE2ETest
 * author         : koo
 * date           : 2026. 10. 2.
 * description    : POST /api/media/qr-dumps/upload E2E 테스트
 */
@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class GenerateQrDumpUploadTicketE2ETest : MediaE2ETestBase() {

    @LocalServerPort
    private var port: Int = 0

    private lateinit var accessToken: String
    private lateinit var testUser: User

    @BeforeEach
    fun setUp() {
        RestAssured.port = port
        RestAssured.baseURI = "http://localhost"

        val (user, token) = createTestUserAndToken()
        testUser = user
        accessToken = token
    }

    @Test
    @DisplayName("qr-dumps/{오늘}/{userId}/{uuid}.html key 로 ticket 을 발급하고 Media 는 만들지 않는다")
    fun givenAuthUser_whenGenerateQrDumpUploadTicket_thenReturnsTicketWithoutMedia() {
        // given
        val today: LocalDate = LocalDate.now(ZoneId.of("Asia/Seoul"))

        // when & then
        RestAssured.given()
            .header("Authorization", "Bearer $accessToken")
            .`when`()
            .post("/api/media/qr-dumps/upload")
            .then()
            .statusCode(HttpStatus.OK.value())
            .body("resultCode", equalTo(ResultCode.SUCCESS.code))
            .body("data.method", equalTo("PUT"))
            .body("data.expiresIn", notNullValue())
            .body("data.contentType", equalTo("text/html; charset=utf-8"))
            .body(
                "data.uploadTicket",
                matchesPattern(".*/qr-dumps/$today/${testUser.id}/[0-9a-f-]{36}\\.html$"),
            )

        assertThat(mediaRepository.count()).isZero()
    }

    @Test
    @DisplayName("토큰이 없는 사용자는 403 에러를 반환한다")
    fun givenNoAuth_whenGenerateQrDumpUploadTicket_thenReturnsForbidden() {
        RestAssured.given()
            .`when`()
            .post("/api/media/qr-dumps/upload")
            .then()
            .statusCode(HttpStatus.FORBIDDEN.value())
    }
}
