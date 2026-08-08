package com.neki.api.e2e.pose

import com.neki.api.pose.api.dto.PoseRequest
import com.neki.core.code.ResultCode
import com.neki.domain.media.models.MediaStatus
import com.neki.domain.pose.models.ScrapPoseId
import com.neki.domain.user.models.User
import io.restassured.RestAssured
import io.restassured.http.ContentType
import org.assertj.core.api.Assertions.assertThat
import org.hamcrest.Matchers.equalTo
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.data.repository.findByIdOrNull
import org.springframework.http.HttpStatus
import org.springframework.test.context.ActiveProfiles

/**
 * fileName       : UpdatePoseScrapE2ETest
 * author         : claude
 * date           : 2026. 1. 29.
 * description    : PATCH /api/poses/{poseId}/scrap E2E 테스트
 */
@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class UpdatePoseScrapE2ETest : PoseE2ETestBase() {

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

    @Nested
    @DisplayName("스크랩 등록 테스트")
    inner class ScrapTests {

        @Test
        @DisplayName("포즈 스크랩 성공")
        fun givenPose_whenScrap_thenReturnSuccess() {
            // given
            val media = createMedia(ownerId = testUser.id!!, status = MediaStatus.UPLOADED)
            val pose = createPose(userId = testUser.id!!, mediaId = media.id!!)

            // when & then
            RestAssured.given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer $accessToken")
                .body(PoseRequest.UpdatePoseScarp(true))
                .`when`()
                .patch("/api/poses/${pose.id}/scrap")
                .then()
                .statusCode(HttpStatus.OK.value())
                .body("resultCode", equalTo(ResultCode.SUCCESS.code))

            val scrapPose = scrapPoseRepository.findByIdOrNull(
                ScrapPoseId(testUser.id!!, pose.id!!),
            )

            assertThat(scrapPose).isNotNull()
        }

        @Test
        @DisplayName("여러 번 스크랩을 등록해도 성공 (멱등성)")
        fun givenPose_whenScrapTwice_thenReturnSuccess() {
            // given
            val media = createMedia(ownerId = testUser.id!!, status = MediaStatus.UPLOADED)
            val pose = createPose(userId = testUser.id!!, mediaId = media.id!!)

            // when & then
            RestAssured.given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer $accessToken")
                .body(PoseRequest.UpdatePoseScarp(true))
                .`when`()
                .patch("/api/poses/${pose.id}/scrap")
                .then()
                .statusCode(HttpStatus.OK.value())
                .body("resultCode", equalTo(ResultCode.SUCCESS.code))

            RestAssured.given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer $accessToken")
                .body(PoseRequest.UpdatePoseScarp(true))
                .`when`()
                .patch("/api/poses/${pose.id}/scrap")
                .then()
                .statusCode(HttpStatus.OK.value())
                .body("resultCode", equalTo(ResultCode.SUCCESS.code))

            val scrapPose = scrapPoseRepository.findByIdOrNull(
                ScrapPoseId(testUser.id!!, pose.id!!),
            )

            assertThat(scrapPose).isNotNull()
        }
    }

    @Nested
    @DisplayName("스크랩 해제 테스트")
    inner class UnscrapTests {

        @Test
        @DisplayName("스크랩하지 않은 포즈를 스크랩 해제하는 경우 성공 (아무 동작도 하지 않음)")
        fun givenPose_whenUnscrap_thenSuccess() {
            // given
            val media = createMedia(ownerId = testUser.id!!, status = MediaStatus.UPLOADED)
            val pose = createPose(userId = testUser.id!!, mediaId = media.id!!)

            // when & then
            RestAssured.given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer $accessToken")
                .body(PoseRequest.UpdatePoseScarp(false))
                .`when`()
                .patch("/api/poses/${pose.id}/scrap")
                .then()
                .statusCode(HttpStatus.OK.value())
                .body("resultCode", equalTo(ResultCode.SUCCESS.code))

            val scrapPose = scrapPoseRepository.findByIdOrNull(
                ScrapPoseId(testUser.id!!, pose.id!!),
            )

            assertThat(scrapPose).isNull()
        }

        @Test
        @DisplayName("스크랩한 포즈를 스크랩에서 해제하면 성공")
        fun givenScrapPose_whenUnscrap_thenSuccess() {
            // given
            val media = createMedia(ownerId = testUser.id!!, status = MediaStatus.UPLOADED)
            val pose = createPose(userId = testUser.id!!, mediaId = media.id!!)

            RestAssured.given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer $accessToken")
                .body(PoseRequest.UpdatePoseScarp(true))
                .`when`()
                .patch("/api/poses/${pose.id}/scrap")
                .then()
                .statusCode(HttpStatus.OK.value())
                .body("resultCode", equalTo(ResultCode.SUCCESS.code))

            // when & then
            RestAssured.given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer $accessToken")
                .body(PoseRequest.UpdatePoseScarp(false))
                .`when`()
                .patch("/api/poses/${pose.id}/scrap")
                .then()
                .statusCode(HttpStatus.OK.value())
                .body("resultCode", equalTo(ResultCode.SUCCESS.code))

            val scrapPose = scrapPoseRepository.findByIdOrNull(
                ScrapPoseId(testUser.id!!, pose.id!!),
            )

            assertThat(scrapPose).isNull()
        }

        @Test
        @DisplayName("스크랩한 포즈를 여러 번 스크랩 해제해도 성공 (멱등성)")
        fun givenScrapPose_whenUnscrapTwice_thenSuccess() {
            // given
            val media = createMedia(ownerId = testUser.id!!, status = MediaStatus.UPLOADED)
            val pose = createPose(userId = testUser.id!!, mediaId = media.id!!)

            RestAssured.given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer $accessToken")
                .body(PoseRequest.UpdatePoseScarp(true))
                .`when`()
                .patch("/api/poses/${pose.id}/scrap")
                .then()
                .statusCode(HttpStatus.OK.value())
                .body("resultCode", equalTo(ResultCode.SUCCESS.code))

            // when & then
            RestAssured.given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer $accessToken")
                .body(PoseRequest.UpdatePoseScarp(false))
                .`when`()
                .patch("/api/poses/${pose.id}/scrap")
                .then()
                .statusCode(HttpStatus.OK.value())
                .body("resultCode", equalTo(ResultCode.SUCCESS.code))

            RestAssured.given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer $accessToken")
                .body(PoseRequest.UpdatePoseScarp(false))
                .`when`()
                .patch("/api/poses/${pose.id}/scrap")
                .then()
                .statusCode(HttpStatus.OK.value())
                .body("resultCode", equalTo(ResultCode.SUCCESS.code))

            val scrapPose = scrapPoseRepository.findByIdOrNull(
                ScrapPoseId(testUser.id!!, pose.id!!),
            )

            assertThat(scrapPose).isNull()
        }
    }

    @Nested
    @DisplayName("실패 케이스")
    inner class FailureTests {

        @Test
        @DisplayName("포즈 스크랩 실패 - 존재하지 않는 포즈")
        fun givenNonExistentPose_whenScrap_thenReturnsNotFound() {
            // given
            val nonExistentPoseId = 999999L

            // when & then
            RestAssured.given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer $accessToken")
                .body(PoseRequest.UpdatePoseScarp(true))
                .`when`()
                .patch("/api/poses/$nonExistentPoseId/scrap")
                .then()
                .statusCode(HttpStatus.BAD_REQUEST.value())
                .body("resultCode", equalTo(ResultCode.NOT_FOUND.code))
        }

        @Test
        @DisplayName("포즈 스크랩 실패 - 인증 없이 요청")
        fun givenNoAuth_whenScrap_thenReturnsForbidden() {
            // given
            val media = createMedia(ownerId = testUser.id!!, status = MediaStatus.UPLOADED)
            val pose = createPose(userId = testUser.id!!, mediaId = media.id!!)

            // when & then
            RestAssured.given()
                .contentType(ContentType.JSON)
                .body(PoseRequest.UpdatePoseScarp(true))
                .`when`()
                .patch("/api/poses/${pose.id}/scrap")
                .then()
                .statusCode(HttpStatus.FORBIDDEN.value())
        }

        @Test
        @DisplayName("포즈 스크랩 실패 - scrap 값 누락")
        fun givenMissingScrapValue_whenScrap_thenReturnsBadRequest() {
            // given
            val media = createMedia(ownerId = testUser.id!!, status = MediaStatus.UPLOADED)
            val pose = createPose(userId = testUser.id!!, mediaId = media.id!!)

            // when & then
            RestAssured.given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer $accessToken")
                .body("{}")
                .`when`()
                .patch("/api/poses/${pose.id}/scrap")
                .then()
                .statusCode(HttpStatus.BAD_REQUEST.value())
        }
    }
}
