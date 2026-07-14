package com.neki.e2e.pose

import com.neki.common.code.ResultCode
import com.neki.media.domain.entity.MediaStatus
import com.neki.pose.domain.HeadCount
import com.neki.user.domain.entity.User
import io.restassured.RestAssured
import org.hamcrest.Matchers.equalTo
import org.hamcrest.Matchers.`in`
import org.hamcrest.Matchers.notNullValue
import org.hamcrest.Matchers.startsWith
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.http.HttpStatus
import org.springframework.test.context.ActiveProfiles

/**
 * fileName       : GetRandomPoseE2ETest
 * author         : claude
 * date           : 2026. 1. 29.
 * description    : GET /api/poses/random E2E 테스트
 */
@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class GetRandomPoseE2ETest : PoseE2ETestBase() {

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
    @DisplayName("성공 케이스")
    inner class SuccessTests {

        @Test
        @DisplayName("랜덤 포즈 조회 성공 - 단일 포즈 존재 시")
        fun givenSinglePose_whenGetRandomPose_thenReturnsPose() {
            // given
            val media = createMedia(ownerId = testUser.id!!, status = MediaStatus.UPLOADED)
            val pose = createPose(userId = testUser.id!!, mediaId = media.id!!, headCount = HeadCount.ONE)

            // when & then
            RestAssured.given()
                .header("Authorization", "Bearer $accessToken")
                .queryParam("headCount", "ONE")
                .`when`()
                .get("/api/poses/random")
                .then()
                .statusCode(HttpStatus.OK.value())
                .body("resultCode", equalTo(ResultCode.SUCCESS.code))
                .body("data.poseId", equalTo(pose.id!!.toInt()))
                .body("data.headCount", equalTo("ONE"))
                .body("data.imageUrl", startsWith("/file/image/"))
                .body("data.scrap", equalTo(false))
                .body("data.contentType", equalTo("image/jpeg"))
                .body("data.createdAt", notNullValue())
        }

        @Test
        @DisplayName("랜덤 포즈 조회 성공 - 여러 포즈 중 하나 반환")
        fun givenMultiplePoses_whenGetRandomPose_thenReturnsOneOfThem() {
            // given
            val poses = (1..5).map {
                val media = createMedia(ownerId = testUser.id!!, status = MediaStatus.UPLOADED)
                createPose(userId = testUser.id!!, mediaId = media.id!!, headCount = HeadCount.TWO)
            }
            val poseIds = poses.map { it.id!!.toInt() }

            // when & then
            RestAssured.given()
                .header("Authorization", "Bearer $accessToken")
                .queryParam("headCount", "TWO")
                .`when`()
                .get("/api/poses/random")
                .then()
                .statusCode(HttpStatus.OK.value())
                .body("resultCode", equalTo(ResultCode.SUCCESS.code))
                .body("data.poseId", `in`(poseIds))
                .body("data.headCount", equalTo("TWO"))
        }

        @Test
        @DisplayName("랜덤 포즈 조회 성공 - 스크랩된 포즈")
        fun givenScrapPose_whenGetRandomPose_thenReturnsWithScrapTrue() {
            // given
            val media = createMedia(ownerId = testUser.id!!, status = MediaStatus.UPLOADED)
            val pose = createPose(userId = testUser.id!!, mediaId = media.id!!, headCount = HeadCount.THREE)
            createScrapPose(userId = testUser.id!!, poseId = pose.id!!)

            // when & then
            RestAssured.given()
                .header("Authorization", "Bearer $accessToken")
                .queryParam("headCount", "THREE")
                .`when`()
                .get("/api/poses/random")
                .then()
                .statusCode(HttpStatus.OK.value())
                .body("resultCode", equalTo(ResultCode.SUCCESS.code))
                .body("data.poseId", equalTo(pose.id!!.toInt()))
                .body("data.scrap", equalTo(true))
        }

        @Test
        @DisplayName("랜덤 포즈 조회 성공 - headCount 필터링 동작 확인")
        fun givenMixedHeadCountPoses_whenGetRandomPoseWithFilter_thenReturnsOnlyMatchingHeadCount() {
            // given
            val mediaOne = createMedia(ownerId = testUser.id!!, status = MediaStatus.UPLOADED)
            createPose(userId = testUser.id!!, mediaId = mediaOne.id!!, headCount = HeadCount.ONE)

            val mediasTwo = (1..3).map {
                val media = createMedia(ownerId = testUser.id!!, status = MediaStatus.UPLOADED)
                createPose(userId = testUser.id!!, mediaId = media.id!!, headCount = HeadCount.TWO)
            }
            val twoHeadCountPoseIds = mediasTwo.map { it.id!!.toInt() }

            // when & then - headCount=TWO로 필터링하면 TWO인 포즈만 반환
            RestAssured.given()
                .header("Authorization", "Bearer $accessToken")
                .queryParam("headCount", "TWO")
                .`when`()
                .get("/api/poses/random")
                .then()
                .statusCode(HttpStatus.OK.value())
                .body("resultCode", equalTo(ResultCode.SUCCESS.code))
                .body("data.poseId", `in`(twoHeadCountPoseIds))
                .body("data.headCount", equalTo("TWO"))
        }
    }

    @Nested
    @DisplayName("실패 케이스")
    inner class FailureTests {

        @Test
        @DisplayName("랜덤 포즈 조회 실패 - 포즈가 없는 경우")
        fun givenNoPoses_whenGetRandomPose_thenReturnsNotFound() {
            // given - no poses created

            // when & then
            RestAssured.given()
                .header("Authorization", "Bearer $accessToken")
                .queryParam("headCount", "ONE")
                .`when`()
                .get("/api/poses/random")
                .then()
                .statusCode(HttpStatus.BAD_REQUEST.value())
                .body("resultCode", equalTo(ResultCode.NO_MORE_RANDOM_POSE.code))
        }

        @Test
        @DisplayName("랜덤 포즈 조회 실패 - 해당 headCount의 포즈가 없는 경우")
        fun givenNoMatchingHeadCountPoses_whenGetRandomPose_thenReturnsNotFound() {
            // given - ONE 포즈만 존재
            val media = createMedia(ownerId = testUser.id!!, status = MediaStatus.UPLOADED)
            createPose(userId = testUser.id!!, mediaId = media.id!!, headCount = HeadCount.ONE)

            // when & then - TWO로 조회하면 NOT_FOUND
            RestAssured.given()
                .header("Authorization", "Bearer $accessToken")
                .queryParam("headCount", "TWO")
                .`when`()
                .get("/api/poses/random")
                .then()
                .statusCode(HttpStatus.BAD_REQUEST.value())
                .body("resultCode", equalTo(ResultCode.NO_MORE_RANDOM_POSE.code))
        }

        @Test
        @DisplayName("랜덤 포즈 조회 실패 - 인증 없이 요청")
        fun givenNoAuth_whenGetRandomPose_thenReturnsForbidden() {
            // when & then
            RestAssured.given()
                .queryParam("headCount", "ONE")
                .`when`()
                .get("/api/poses/random")
                .then()
                .statusCode(HttpStatus.FORBIDDEN.value())
        }
    }
}
