package com.neki.api.e2e.support

import com.neki.api.e2e.E2ETestBase
import com.neki.core.code.ResultCode
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
import java.util.UUID

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
    @DisplayName("인증된 행동 이벤트의 발생 시점 정보와 인증 시점 userId 를 따로 전달한다")
    fun givenLogs_whenCollect_thenSentWithUserId() {
        val logs = listOf(event(userIdAtOccurrence = "999", properties = mapOf("count" to 2)))

        post(mapOf("platform" to "IOS", "logs" to logs))
            .statusCode(HttpStatus.OK.value())
            .body("resultCode", equalTo(ResultCode.SUCCESS.code))

        val sent = fakeClientLogSender.sent.single()
        assertEquals(testUser.id, sent.userId)
        assertEquals(Platform.IOS, sent.platform)
        assertEquals("999", sent.logs.single().userIdAtOccurrence)
        assertEquals(1_790_935_140_000, sent.logs.single().sessionId)
        assertEquals(1, sent.logs.single().sequenceNumber)
        assertEquals(mapOf("count" to 2), sent.logs.single().eventProperties)
    }

    @Test
    @DisplayName("토큰 없는 익명 행동 이벤트도 받되 인증 userId 는 비운다")
    fun givenNoToken_whenCollectThenAnonymous() {
        RestAssured.given()
            .contentType(ContentType.JSON)
            .body(mapOf("platform" to "IOS", "logs" to listOf(event())))
            .`when`()
            .post("/api/logs")
            .then()
            .statusCode(HttpStatus.OK.value())
            .body("resultCode", equalTo(ResultCode.SUCCESS.code))

        assertEquals(null, fakeClientLogSender.sent.single().userId)
    }

    @Test
    @DisplayName("잘못된 Authorization 형식은 익명으로 통과시키지 않는다")
    fun givenMalformedAuthorizationThenRejected() {
        RestAssured.given()
            .contentType(ContentType.JSON)
            .header("Authorization", "Basic malformed")
            .body(mapOf("platform" to "IOS", "logs" to listOf(event())))
            .`when`()
            .post("/api/logs")
            .then()
            .statusCode(HttpStatus.FORBIDDEN.value())
            .body("resultCode", equalTo(ResultCode.INVALID_TOKEN_ERROR.code))

        assertTrue(fakeClientLogSender.sent.isEmpty())
    }

    @Test
    @DisplayName("로그가 비었거나 500건을 넘으면 400 을 반환한다")
    fun givenEmptyOrTooManyLogs_whenCollect_thenBadRequest() {
        listOf(emptyList(), List(501) { event() }).forEach { logs ->
            post(mapOf("platform" to "ANDROID", "logs" to logs))
                .statusCode(HttpStatus.BAD_REQUEST.value())
                .body("resultCode", equalTo(ResultCode.INVALID_PARAMETER.code))
        }

        assertTrue(fakeClientLogSender.sent.isEmpty())
    }

    @Test
    @DisplayName("지원하지 않는 플랫폼이나 필수 이벤트 필드 누락은 400 을 반환한다")
    fun givenInvalidPlatformOrNonObjectLog_whenCollect_thenBadRequest() {
        post(mapOf("platform" to "WEB", "logs" to listOf(event())))
            .statusCode(HttpStatus.BAD_REQUEST.value())
        post(mapOf("platform" to "IOS", "logs" to listOf(event() - "sessionId")))
            .statusCode(HttpStatus.BAD_REQUEST.value())
        post(mapOf("platform" to "IOS", "logs" to listOf(event() + ("eventType" to ""))))
            .statusCode(HttpStatus.BAD_REQUEST.value())
        post(mapOf("platform" to "IOS", "logs" to listOf(event() + ("sequenceNumber" to 0))))
            .statusCode(HttpStatus.BAD_REQUEST.value())

        assertTrue(fakeClientLogSender.sent.isEmpty())
    }

    private fun event(
        userIdAtOccurrence: String? = null,
        properties: Map<String, Any?> = emptyMap(),
    ): Map<String, Any?> = mapOf(
        "eventId" to UUID.randomUUID().toString(),
        "eventType" to "photo_upload",
        "eventProperties" to properties,
        "occurredAt" to "2026-10-02T10:00:00Z",
        "sessionId" to 1_790_935_140_000,
        "sequenceNumber" to 1,
        "deviceId" to "device-12345",
        "userIdAtOccurrence" to userIdAtOccurrence,
        "appVersion" to "1.4.0",
        "context" to mapOf("osName" to "iOS", "osVersion" to "18.0"),
    )

    private fun post(body: Map<String, Any>) = RestAssured.given()
        .contentType(ContentType.JSON)
        .header("Authorization", "Bearer $accessToken")
        .body(body)
        .`when`()
        .post("/api/logs")
        .then()
}
