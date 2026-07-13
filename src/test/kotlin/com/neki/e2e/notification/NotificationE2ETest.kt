package com.neki.e2e.notification

import com.neki.common.code.ResultCode
import com.neki.e2e.E2ETestBase
import com.neki.notification.api.dto.UpdateNotificationRequest
import com.neki.notification.domain.entity.Notification
import com.neki.notification.infra.persist.jpa.JpaNotificationRepository
import com.neki.user.domain.entity.User
import io.restassured.RestAssured
import io.restassured.http.ContentType
import org.hamcrest.CoreMatchers.equalTo
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.http.HttpStatus
import org.springframework.test.context.ActiveProfiles

/**
 * fileName       : NotificationE2ETest
 * author         : darren
 * date           : 2026. 6. 12
 * description    : 알림 등록/수정 API E2E 테스트
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class NotificationE2ETest : E2ETestBase() {

    @LocalServerPort
    private var port: Int = 0

    @Autowired
    private lateinit var jpaNotificationRepository: JpaNotificationRepository

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
        jpaNotificationRepository.deleteAllInBatch()
        super.tearDown()
    }

    @Test
    @DisplayName("알림 토큰과 푸시 동의 여부를 등록하면 성공하고 DB에 저장된다")
    fun givenValidRequest_whenUpdateNotification_thenPersisted() {
        RestAssured.given()
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer $accessToken")
            .body(UpdateNotificationRequest(deviceToken = "device-token-123", pushAgreed = true))
            .`when`()
            .patch("/api/notifications")
            .then()
            .statusCode(HttpStatus.OK.value())
            .body("resultCode", equalTo(ResultCode.SUCCESS.code))

        val saved: Notification? = jpaNotificationRepository.findByUserId(testUser.id!!)
        assertNotNull(saved)
        assertEquals("device-token-123", saved!!.deviceToken)
        assertEquals(true, saved.pushAgreed)
    }

    @Test
    @DisplayName("이미 등록된 사용자가 다시 요청하면 새 row 생성 없이 기존 값이 갱신된다")
    fun givenExistingNotification_whenUpdateAgain_thenUpserted() {
        updateNotification("old-token", false)
        updateNotification("new-token", true)

        val userRows: List<Notification> =
            jpaNotificationRepository.findAll().filter { it.userId == testUser.id }
        assertEquals(1, userRows.size)

        val saved: Notification = userRows.first()
        assertEquals("new-token", saved.deviceToken)
        assertEquals(true, saved.pushAgreed)
    }

    @Test
    @DisplayName("푸시 동의 여부를 생략하면 기본값 false로 저장된다")
    fun givenNoPushAgreed_whenUpdateNotification_thenDefaultsToFalse() {
        RestAssured.given()
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer $accessToken")
            .body(mapOf("deviceToken" to "device-token-only"))
            .`when`()
            .patch("/api/notifications")
            .then()
            .statusCode(HttpStatus.OK.value())

        val saved: Notification? = jpaNotificationRepository.findByUserId(testUser.id!!)
        assertNotNull(saved)
        assertEquals(false, saved!!.pushAgreed)
    }

    @Test
    @DisplayName("알림 토큰이 비어 있으면 400 에러를 반환한다")
    fun givenBlankDeviceToken_whenUpdateNotification_thenReturnsBadRequest() {
        RestAssured.given()
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer $accessToken")
            .body(UpdateNotificationRequest(deviceToken = "", pushAgreed = true))
            .`when`()
            .patch("/api/notifications")
            .then()
            .statusCode(HttpStatus.BAD_REQUEST.value())
            .body("resultCode", equalTo(ResultCode.INVALID_PARAMETER.code))
    }

    @Test
    @DisplayName("토큰 없이 알림 등록 시 403 에러를 반환한다")
    fun givenNoToken_whenUpdateNotification_thenReturnsForbidden() {
        RestAssured.given()
            .contentType(ContentType.JSON)
            .body(UpdateNotificationRequest(deviceToken = "device-token-123", pushAgreed = true))
            .`when`()
            .patch("/api/notifications")
            .then()
            .statusCode(HttpStatus.FORBIDDEN.value())
    }

    @Test
    @DisplayName("알림 등록 후 사용자 정보 조회 시 푸시 동의 여부가 반영된다")
    fun givenRegisteredNotification_whenGetUserInfo_thenReflectsPushAgreed() {
        updateNotification("device-token-123", true)

        RestAssured.given()
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer $accessToken")
            .`when`()
            .get("/api/users/info")
            .then()
            .statusCode(HttpStatus.OK.value())
            .body("data.pushAgreed", equalTo(true))
    }

    @Test
    @DisplayName("알림 미등록 사용자의 정보 조회 시 푸시 동의 여부는 false로 반환된다")
    fun givenNoNotification_whenGetUserInfo_thenPushAgreedIsFalse() {
        RestAssured.given()
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer $accessToken")
            .`when`()
            .get("/api/users/info")
            .then()
            .statusCode(HttpStatus.OK.value())
            .body("data.pushAgreed", equalTo(false))
    }

    private fun updateNotification(deviceToken: String, pushAgreed: Boolean) {
        RestAssured.given()
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer $accessToken")
            .body(UpdateNotificationRequest(deviceToken = deviceToken, pushAgreed = pushAgreed))
            .`when`()
            .patch("/api/notifications")
            .then()
            .statusCode(HttpStatus.OK.value())
    }
}
