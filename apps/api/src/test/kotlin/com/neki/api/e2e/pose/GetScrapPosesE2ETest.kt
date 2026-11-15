package com.neki.api.e2e.pose

import com.neki.core.code.ResultCode
import com.neki.domain.media.models.MediaStatus
import com.neki.domain.user.models.User
import io.restassured.RestAssured
import org.hamcrest.Matchers.empty
import org.hamcrest.Matchers.equalTo
import org.hamcrest.Matchers.hasSize
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.http.HttpStatus
import org.springframework.test.context.ActiveProfiles

/**
 * fileName       : GetScrapPosesE2ETest
 * author         : darren
 * date           : 2026. 11. 15.
 * description    : GET /api/poses/scrap E2E 테스트
 */
@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class GetScrapPosesE2ETest : PoseE2ETestBase() {

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
    @DisplayName("totalCount 는 현재 페이지가 아니라 내가 스크랩한 전체 포즈 수다")
    fun givenScraps_whenGetFirstPage_thenTotalCountIsAllMyScraps() {
        // given: 내 스크랩 3개, 스크랩하지 않은 포즈 1개, 다른 사용자의 스크랩 1개
        val (otherUser, _) = createTestUserAndToken(email = "other-${System.currentTimeMillis()}@example.com")
        val poses = (1..5).map {
            val media = createMedia(ownerId = testUser.id!!, status = MediaStatus.UPLOADED)
            createPose(userId = testUser.id!!, mediaId = media.id!!)
        }
        poses.take(3).forEach { createScrapPose(userId = testUser.id!!, poseId = it.id!!) }
        createScrapPose(userId = otherUser.id!!, poseId = poses[4].id!!)

        // when & then
        RestAssured.given()
            .header("Authorization", "Bearer $accessToken")
            .queryParam("page", 0)
            .queryParam("size", 2)
            .`when`()
            .get("/api/poses/scrap")
            .then()
            .statusCode(HttpStatus.OK.value())
            .body("resultCode", equalTo(ResultCode.SUCCESS.code))
            .body("data.items", hasSize<Int>(2))
            .body("data.hasNext", equalTo(true))
            .body("data.totalCount", equalTo(3))
    }

    @Test
    @DisplayName("스크랩이 없으면 빈 목록과 totalCount 0")
    fun givenNoScrap_whenGet_thenEmptyAndZero() {
        RestAssured.given()
            .header("Authorization", "Bearer $accessToken")
            .`when`()
            .get("/api/poses/scrap")
            .then()
            .statusCode(HttpStatus.OK.value())
            .body("data.items", empty<Any>())
            .body("data.hasNext", equalTo(false))
            .body("data.totalCount", equalTo(0))
    }
}
