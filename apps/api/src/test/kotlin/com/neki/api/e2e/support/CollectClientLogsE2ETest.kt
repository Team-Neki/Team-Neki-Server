package com.neki.api.e2e.support

import com.neki.api.e2e.E2ETestBase
import com.neki.core.code.ResultCode
import com.neki.domain.support.dto.ClientLogCommand
import com.neki.domain.support.infra.firehose.fake.FakeClientLogSender
import com.neki.domain.support.models.Platform
import com.neki.domain.user.models.User
import io.restassured.RestAssured
import io.restassured.http.ContentType
import org.hamcrest.CoreMatchers.equalTo
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.http.HttpStatus
import org.springframework.test.context.ActiveProfiles

/**
 * fileName       : CollectClientLogsE2ETest
 * author         : koo
 * date           : 2026. 10. 2.
 * description    : 클라이언트 로그 수집 API E2E 테스트
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class CollectClientLogsE2ETest : E2ETestBase() {

    @LocalServerPort
    private var port: Int = 0

    @Autowired
    private lateinit var fakeClientLogSender: FakeClientLogSender

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

    @AfterEach
    override fun tearDown() {
        fakeClientLogSender.sent.clear()
        super.tearDown()
    }

    @Test
    @DisplayName("로그 배치를 보내면 토큰의 userId 와 함께 원본 JSON 그대로 전달된다")
    fun givenLogs_whenCollect_thenSentWithUserId() {
        val logs: List<Map<String, Any?>> = listOf(
            mapOf("level" to "ERROR", "message" to "upload failed", "extra" to mapOf("code" to 500)),
            mapOf("event" to "tap_pose"),
        )

        post(mapOf("platform" to "IOS", "appVersion" to "1.4.0", "logs" to logs))
            .statusCode(HttpStatus.OK.value())
            .body("resultCode", equalTo(ResultCode.SUCCESS.code))

        assertEquals(
            listOf(ClientLogCommand.Collect(testUser.id!!, Platform.IOS, "1.4.0", logs)),
            fakeClientLogSender.sent,
        )
    }

    @Test
    @DisplayName("토큰 없이 요청하면 403 으로 거부되고 전달하지 않는다")
    fun givenNoToken_whenCollect_thenRejected() {
        RestAssured.given()
            .contentType(ContentType.JSON)
            .body(mapOf("platform" to "IOS", "appVersion" to "1.4.0", "logs" to listOf(mapOf("a" to 1))))
            .`when`()
            .post("/api/logs")
            .then()
            .statusCode(HttpStatus.FORBIDDEN.value())
            .body("resultCode", equalTo(ResultCode.MISSING_TOKEN_ERROR.code))

        assertTrue(fakeClientLogSender.sent.isEmpty())
    }

    @Test
    @DisplayName("로그가 비었거나 500건을 넘으면 400 을 반환한다")
    fun givenEmptyOrTooManyLogs_whenCollect_thenBadRequest() {
        listOf(emptyList(), List(501) { mapOf("i" to it) }).forEach { logs ->
            post(mapOf("platform" to "ANDROID", "appVersion" to "1.4.0", "logs" to logs))
                .statusCode(HttpStatus.BAD_REQUEST.value())
                .body("resultCode", equalTo(ResultCode.INVALID_PARAMETER.code))
        }

        assertTrue(fakeClientLogSender.sent.isEmpty())
    }

    @Test
    @DisplayName("지원하지 않는 플랫폼이거나 로그 원소가 객체가 아니면 400 을 반환한다")
    fun givenInvalidPlatformOrNonObjectLog_whenCollect_thenBadRequest() {
        post(mapOf("platform" to "WEB", "appVersion" to "1.4.0", "logs" to listOf(mapOf("a" to 1))))
            .statusCode(HttpStatus.BAD_REQUEST.value())
        post(mapOf("platform" to "IOS", "appVersion" to "1.4.0", "logs" to listOf("plain string")))
            .statusCode(HttpStatus.BAD_REQUEST.value())

        assertTrue(fakeClientLogSender.sent.isEmpty())
    }

    private fun post(body: Map<String, Any>) = RestAssured.given()
        .contentType(ContentType.JSON)
        .header("Authorization", "Bearer $accessToken")
        .body(body)
        .`when`()
        .post("/api/logs")
        .then()
}
