package com.neki.e2e.pose

import com.neki.common.code.ResultCode
import com.neki.media.models.MediaStatus
import com.neki.pose.models.HeadCount
import com.neki.user.models.User
import io.restassured.RestAssured
import org.assertj.core.api.Assertions.assertThat
import org.hamcrest.Matchers.equalTo
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
 * fileName       : GetPoseE2ETest
 * author         : claude
 * date           : 2026. 1. 29.
 * description    : GET /api/poses/{poseId} E2E 테스트
 */
@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class GetPoseE2ETest : PoseE2ETestBase() {

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
        @DisplayName("포즈 상세 조회 성공 - 기본 조회")
        fun givenPoseExists_whenGetPose_thenReturnsPoseDetail() {
            // given
            val media = createMedia(ownerId = testUser.id!!, status = MediaStatus.UPLOADED)
            val pose = createPose(userId = testUser.id!!, mediaId = media.id!!, headCount = HeadCount.TWO)

            // when & then
            RestAssured.given()
                .header("Authorization", "Bearer $accessToken")
                .`when`()
                .get("/api/poses/${pose.id}")
                .then()
                .statusCode(HttpStatus.OK.value())
                .body("resultCode", equalTo(ResultCode.SUCCESS.code))
                .body("data.poseId", equalTo(pose.id!!.toInt()))
                .body("data.headCount", equalTo("TWO"))
                .body("data.imageUrl", startsWith("/file/image/"))
                .body("data.scrap", equalTo(false))
                .body("data.contentType", equalTo("image/jpeg"))
                .body("data.createdAt", notNullValue())
        }

        @Test
        @DisplayName("포즈 상세 조회 성공 - 스크랩된 포즈")
        fun givenScrapPose_whenGetPose_thenReturnsScrapTrue() {
            // given
            val media = createMedia(ownerId = testUser.id!!, status = MediaStatus.UPLOADED)
            val pose = createPose(userId = testUser.id!!, mediaId = media.id!!, headCount = HeadCount.ONE)
            createScrapPose(userId = testUser.id!!, poseId = pose.id!!)

            // when & then
            RestAssured.given()
                .header("Authorization", "Bearer $accessToken")
                .`when`()
                .get("/api/poses/${pose.id}")
                .then()
                .statusCode(HttpStatus.OK.value())
                .body("resultCode", equalTo(ResultCode.SUCCESS.code))
                .body("data.poseId", equalTo(pose.id!!.toInt()))
                .body("data.scrap", equalTo(true))
        }

        @Test
        @DisplayName("포즈 상세 조회 성공 - 조회수 증가")
        fun givenPoseExists_whenGetPose_thenViewCountIncreases() {
            // given
            val media = createMedia(ownerId = testUser.id!!, status = MediaStatus.UPLOADED)
            val pose = createPose(userId = testUser.id!!, mediaId = media.id!!, headCount = HeadCount.ONE)

            // when
            RestAssured.given()
                .header("Authorization", "Bearer $accessToken")
                .`when`()
                .get("/api/poses/${pose.id}")
                .then()
                .statusCode(HttpStatus.OK.value())

            // then
            val updated = poseRepository.findById(pose.id!!).get()
            assertThat(updated.viewCount).isEqualTo(1)
        }

        @Test
        @DisplayName("포즈 상세 조회 성공 - 동일 유저 중복 조회 시 조회수 미증가")
        fun givenSameUser_whenGetPoseTwice_thenViewCountIncreasesOnce() {
            // given
            val media = createMedia(ownerId = testUser.id!!, status = MediaStatus.UPLOADED)
            val pose = createPose(userId = testUser.id!!, mediaId = media.id!!, headCount = HeadCount.ONE)

            // when: 같은 유저가 두 번 조회
            RestAssured.given()
                .header("Authorization", "Bearer $accessToken")
                .`when`()
                .get("/api/poses/${pose.id}")
                .then()
                .statusCode(HttpStatus.OK.value())

            RestAssured.given()
                .header("Authorization", "Bearer $accessToken")
                .`when`()
                .get("/api/poses/${pose.id}")
                .then()
                .statusCode(HttpStatus.OK.value())

            // then: 조회수는 1만 증가
            val updated = poseRepository.findById(pose.id!!).get()
            assertThat(updated.viewCount).isEqualTo(1)
        }

        @Test
        @DisplayName("포즈 상세 조회 성공 - 다양한 headCount")
        fun givenPoseWithFiveOrMore_whenGetPose_thenReturnsPoseDetail() {
            // given
            val media = createMedia(ownerId = testUser.id!!, status = MediaStatus.UPLOADED)
            val pose = createPose(userId = testUser.id!!, mediaId = media.id!!, headCount = HeadCount.FIVE_OR_MORE)

            // when & then
            RestAssured.given()
                .header("Authorization", "Bearer $accessToken")
                .`when`()
                .get("/api/poses/${pose.id}")
                .then()
                .statusCode(HttpStatus.OK.value())
                .body("resultCode", equalTo(ResultCode.SUCCESS.code))
                .body("data.poseId", equalTo(pose.id!!.toInt()))
                .body("data.headCount", equalTo("FIVE_OR_MORE"))
        }
    }

    @Nested
    @DisplayName("실패 케이스")
    inner class FailureTests {

        @Test
        @DisplayName("포즈 상세 조회 실패 - 존재하지 않는 포즈")
        fun givenNonExistentPose_whenGetPose_thenReturnsNotFound() {
            // given
            val nonExistentPoseId = 999999L

            // when & then
            RestAssured.given()
                .header("Authorization", "Bearer $accessToken")
                .`when`()
                .get("/api/poses/$nonExistentPoseId")
                .then()
                .statusCode(HttpStatus.BAD_REQUEST.value())
                .body("resultCode", equalTo(ResultCode.NOT_FOUND.code))
        }

        @Test
        @DisplayName("포즈 상세 조회 실패 - 인증 없이 요청")
        fun givenNoAuth_whenGetPose_thenReturnsForbidden() {
            // given
            val media = createMedia(ownerId = testUser.id!!, status = MediaStatus.UPLOADED)
            val pose = createPose(userId = testUser.id!!, mediaId = media.id!!)

            // when & then
            RestAssured.given()
                .`when`()
                .get("/api/poses/${pose.id}")
                .then()
                .statusCode(HttpStatus.FORBIDDEN.value())
        }
    }
}
