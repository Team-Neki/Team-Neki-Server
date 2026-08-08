package com.neki.api.e2e.notification

import com.neki.api.e2e.E2ETestBase
import com.neki.api.notification.infra.persist.jpa.JpaNotificationHistRepository
import com.neki.core.code.ResultCode
import com.neki.domain.notification.models.NotificationHist
import com.neki.domain.user.models.User
import io.restassured.RestAssured
import io.restassured.http.ContentType
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.Matchers.contains
import org.hamcrest.Matchers.hasSize
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.http.HttpStatus
import org.springframework.test.context.ActiveProfiles

/**
 * fileName       : GetRecentNotificationsE2ETest
 * author         : darren
 * date           : 2026. 6. 22
 * description    : 최근 알림 조회 API E2E 테스트
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class GetRecentNotificationsE2ETest : E2ETestBase() {

    @LocalServerPort
    private var port: Int = 0

    @Autowired
    private lateinit var notificationHistRepository: JpaNotificationHistRepository

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
        notificationHistRepository.deleteAllInBatch()
        super.tearDown()
    }

    @Test
    @DisplayName("최근 알림이 없으면 빈 목록을 반환한다")
    fun givenNoHist_whenGetRecent_thenReturnsEmptyList() {
        RestAssured.given()
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer $accessToken")
            .`when`()
            .get("/api/notifications/recent")
            .then()
            .statusCode(HttpStatus.OK.value())
            .body("resultCode", equalTo(ResultCode.SUCCESS.code))
            .body("data", hasSize<Any>(0))
    }

    @Test
    @DisplayName("최근 알림을 최신순(가장 최근에 온 알림이 먼저)으로 반환한다")
    fun givenHists_whenGetRecent_thenReturnsInLatestOrder() {
        // given: 순서대로 저장 (나중에 저장된 것이 더 최근)
        saveHist(testUser.id!!, type = "ARCHIVE", title = "첫 번째 알림")
        saveHist(testUser.id!!, type = "MARKETING", title = "두 번째 알림")
        saveHist(testUser.id!!, type = "ARCHIVE", title = "세 번째 알림")

        // when & then: 최신순으로 반환
        RestAssured.given()
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer $accessToken")
            .`when`()
            .get("/api/notifications/recent")
            .then()
            .statusCode(HttpStatus.OK.value())
            .body("data", hasSize<Any>(3))
            .body("data.title", contains("세 번째 알림", "두 번째 알림", "첫 번째 알림"))
    }

    @Test
    @DisplayName("최근 알림은 최대 30건까지만 반환한다")
    fun givenMoreThan30Hists_whenGetRecent_thenReturnsAtMost30() {
        // given: 35건 저장
        repeat(35) { index ->
            saveHist(testUser.id!!, type = "ARCHIVE", title = "알림-$index")
        }

        // when & then: 30건만 반환
        RestAssured.given()
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer $accessToken")
            .`when`()
            .get("/api/notifications/recent")
            .then()
            .statusCode(HttpStatus.OK.value())
            .body("data", hasSize<Any>(30))
            // 가장 최근(마지막에 저장한 알림-34)이 첫 번째
            .body("data[0].title", equalTo("알림-34"))
            // 30건 제한으로 가장 오래된 알림-0 ~ 알림-4 는 제외, 경계값 알림-5 가 마지막
            .body("data[29].title", equalTo("알림-5"))
    }

    @Test
    @DisplayName("다른 사용자의 알림은 조회되지 않는다")
    fun givenOtherUsersHist_whenGetRecent_thenExcludesThem() {
        // given: 다른 사용자의 알림
        val (otherUser, _) = createTestUserAndToken(email = "other-${System.currentTimeMillis()}@example.com")
        saveHist(otherUser.id!!, type = "ARCHIVE", title = "남의 알림")
        // 내 알림
        saveHist(testUser.id!!, type = "ARCHIVE", title = "내 알림")

        // when & then: 내 알림만 반환
        RestAssured.given()
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer $accessToken")
            .`when`()
            .get("/api/notifications/recent")
            .then()
            .statusCode(HttpStatus.OK.value())
            .body("data", hasSize<Any>(1))
            .body("data[0].title", equalTo("내 알림"))
    }

    @Test
    @DisplayName("토큰 없이 최근 알림 조회 시 403 에러를 반환한다")
    fun givenNoToken_whenGetRecent_thenReturnsForbidden() {
        RestAssured.given()
            .contentType(ContentType.JSON)
            .`when`()
            .get("/api/notifications/recent")
            .then()
            .statusCode(HttpStatus.FORBIDDEN.value())
    }

    private fun saveHist(userId: Long, type: String, title: String) {
        notificationHistRepository.save(
            NotificationHist(
                userId = userId,
                type = type,
                title = title,
                body = "$title 내용",
                link = "neki://archive/1",
            ),
        )
    }
}
