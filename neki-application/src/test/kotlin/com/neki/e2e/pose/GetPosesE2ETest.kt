package com.neki.e2e.pose

import com.neki.common.api.dto.ResultCode
import com.neki.media.entity.MediaStatus
import com.neki.pose.HeadCount
import com.neki.user.entity.User
import io.restassured.RestAssured
import org.hamcrest.Matchers.empty
import org.hamcrest.Matchers.equalTo
import org.hamcrest.Matchers.hasSize
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
 * fileName       : GetPosesE2ETest
 * author         : claude
 * date           : 2026. 1. 29.
 * description    : GET /api/poses E2E 테스트
 */
@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class GetPosesE2ETest : PoseE2ETestBase() {

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
        @DisplayName("포즈 목록 조회 성공 - 전체 포즈 조회 및 imageUrl이 /file/image/ 패턴을 따름")
        fun givenPosesExist_whenGetPoses_thenReturnsPoseListWithFileImageUrl() {
            // given
            val media1 = createMedia(ownerId = testUser.id!!, status = MediaStatus.UPLOADED)
            val media2 = createMedia(ownerId = testUser.id!!, status = MediaStatus.UPLOADED)
            createPose(userId = testUser.id!!, mediaId = media1.id!!, headCount = HeadCount.ONE)
            createPose(userId = testUser.id!!, mediaId = media2.id!!, headCount = HeadCount.TWO)

            // when & then
            RestAssured.given()
                .header("Authorization", "Bearer $accessToken")
                .`when`()
                .get("/api/poses")
                .then()
                .statusCode(HttpStatus.OK.value())
                .body("resultCode", equalTo(ResultCode.SUCCESS.code))
                .body("data.items", hasSize<Int>(2))
                .body("data.items[0].imageUrl", startsWith("/file/image/"))
                .body("data.items[1].imageUrl", startsWith("/file/image/"))
        }

        @Test
        @DisplayName("포즈 목록 조회 성공 - 포즈가 없는 경우 빈 목록 반환")
        fun givenNoPoses_whenGetPoses_thenReturnsEmptyList() {
            // given - no poses created

            // when & then
            RestAssured.given()
                .header("Authorization", "Bearer $accessToken")
                .`when`()
                .get("/api/poses")
                .then()
                .statusCode(HttpStatus.OK.value())
                .body("resultCode", equalTo(ResultCode.SUCCESS.code))
                .body("data.items", empty<Any>())
        }
    }

    @Nested
    @DisplayName("HeadCount 필터링 테스트")
    inner class HeadCountFilterTests {

        @Test
        @DisplayName("포즈 목록 조회 - headCount=ONE 필터링")
        fun givenMultiplePoses_whenGetPosesWithHeadCountOne_thenReturnsFilteredPoses() {
            // given
            val media1 = createMedia(ownerId = testUser.id!!, status = MediaStatus.UPLOADED)
            val media2 = createMedia(ownerId = testUser.id!!, status = MediaStatus.UPLOADED)
            val media3 = createMedia(ownerId = testUser.id!!, status = MediaStatus.UPLOADED)

            createPose(userId = testUser.id!!, mediaId = media1.id!!, headCount = HeadCount.ONE)
            createPose(userId = testUser.id!!, mediaId = media2.id!!, headCount = HeadCount.TWO)
            createPose(userId = testUser.id!!, mediaId = media3.id!!, headCount = HeadCount.ONE)

            // when & then
            RestAssured.given()
                .header("Authorization", "Bearer $accessToken")
                .queryParam("headCount", "ONE")
                .`when`()
                .get("/api/poses")
                .then()
                .statusCode(HttpStatus.OK.value())
                .body("resultCode", equalTo(ResultCode.SUCCESS.code))
                .body("data.items", hasSize<Int>(2))
                .body("data.items[0].headCount", equalTo("ONE"))
                .body("data.items[1].headCount", equalTo("ONE"))
        }

        @Test
        @DisplayName("포즈 목록 조회 - headCount=TWO 필터링")
        fun givenMultiplePoses_whenGetPosesWithHeadCountTwo_thenReturnsFilteredPoses() {
            // given
            val media1 = createMedia(ownerId = testUser.id!!, status = MediaStatus.UPLOADED)
            val media2 = createMedia(ownerId = testUser.id!!, status = MediaStatus.UPLOADED)

            createPose(userId = testUser.id!!, mediaId = media1.id!!, headCount = HeadCount.ONE)
            createPose(userId = testUser.id!!, mediaId = media2.id!!, headCount = HeadCount.TWO)

            // when & then
            RestAssured.given()
                .header("Authorization", "Bearer $accessToken")
                .queryParam("headCount", "TWO")
                .`when`()
                .get("/api/poses")
                .then()
                .statusCode(HttpStatus.OK.value())
                .body("resultCode", equalTo(ResultCode.SUCCESS.code))
                .body("data.items", hasSize<Int>(1))
                .body("data.items[0].headCount", equalTo("TWO"))
        }

        @Test
        @DisplayName("포즈 목록 조회 - headCount=FIVE_OR_MORE 필터링")
        fun givenMultiplePoses_whenGetPosesWithHeadCountFiveOrMore_thenReturnsFilteredPoses() {
            // given
            val media1 = createMedia(ownerId = testUser.id!!, status = MediaStatus.UPLOADED)
            val media2 = createMedia(ownerId = testUser.id!!, status = MediaStatus.UPLOADED)

            createPose(userId = testUser.id!!, mediaId = media1.id!!, headCount = HeadCount.ONE)
            createPose(userId = testUser.id!!, mediaId = media2.id!!, headCount = HeadCount.FIVE_OR_MORE)

            // when & then
            RestAssured.given()
                .header("Authorization", "Bearer $accessToken")
                .queryParam("headCount", "FIVE_OR_MORE")
                .`when`()
                .get("/api/poses")
                .then()
                .statusCode(HttpStatus.OK.value())
                .body("resultCode", equalTo(ResultCode.SUCCESS.code))
                .body("data.items", hasSize<Int>(1))
                .body("data.items[0].headCount", equalTo("FIVE_OR_MORE"))
        }

        @Test
        @DisplayName("포즈 목록 조회 - headCount 필터에 해당하는 포즈가 없는 경우")
        fun givenPosesWithDifferentHeadCount_whenFilterByHeadCount_thenReturnsEmptyList() {
            // given
            val media = createMedia(ownerId = testUser.id!!, status = MediaStatus.UPLOADED)
            createPose(userId = testUser.id!!, mediaId = media.id!!, headCount = HeadCount.ONE)

            // when & then
            RestAssured.given()
                .header("Authorization", "Bearer $accessToken")
                .queryParam("headCount", "FOUR")
                .`when`()
                .get("/api/poses")
                .then()
                .statusCode(HttpStatus.OK.value())
                .body("resultCode", equalTo(ResultCode.SUCCESS.code))
                .body("data.items", empty<Any>())
        }
    }

    @Nested
    @DisplayName("페이징 테스트")
    inner class PagingTests {

        @Test
        @DisplayName("포즈 목록 페이징 조회 - 첫 페이지")
        fun givenMultiplePoses_whenGetFirstPage_thenReturnsPagedResults() {
            // given: 3개의 포즈 생성
            repeat(3) {
                val media = createMedia(ownerId = testUser.id!!, status = MediaStatus.UPLOADED)
                createPose(userId = testUser.id!!, mediaId = media.id!!)
            }

            // when & then: page=0, size=2 조회 시 2개 반환 + hasNext=true
            RestAssured.given()
                .header("Authorization", "Bearer $accessToken")
                .queryParam("page", 0)
                .queryParam("size", 2)
                .`when`()
                .get("/api/poses")
                .then()
                .statusCode(HttpStatus.OK.value())
                .body("resultCode", equalTo(ResultCode.SUCCESS.code))
                .body("data.items", hasSize<Int>(2))
                .body("data.hasNext", equalTo(true))
        }

        @Test
        @DisplayName("포즈 목록 페이징 조회 - 마지막 페이지")
        fun givenMultiplePoses_whenGetLastPage_thenReturnsRemainingResults() {
            // given: 3개의 포즈 생성
            repeat(3) {
                val media = createMedia(ownerId = testUser.id!!, status = MediaStatus.UPLOADED)
                createPose(userId = testUser.id!!, mediaId = media.id!!)
            }

            // when & then: page=1, size=2 조회 시 1개 반환 + hasNext=false
            RestAssured.given()
                .header("Authorization", "Bearer $accessToken")
                .queryParam("page", 1)
                .queryParam("size", 2)
                .`when`()
                .get("/api/poses")
                .then()
                .statusCode(HttpStatus.OK.value())
                .body("resultCode", equalTo(ResultCode.SUCCESS.code))
                .body("data.items", hasSize<Int>(1))
                .body("data.hasNext", equalTo(false))
        }

        @Test
        @DisplayName("포즈 목록 페이징 조회 - 기본 파라미터")
        fun givenMultiplePoses_whenGetWithoutPagination_thenReturnsDefaultPageSize() {
            // given: 25개의 포즈 생성 (default size=20보다 많음)
            repeat(25) {
                val media = createMedia(ownerId = testUser.id!!, status = MediaStatus.UPLOADED)
                createPose(userId = testUser.id!!, mediaId = media.id!!)
            }

            // when & then: 파라미터 없이 조회 시 20개 반환 + hasNext=true
            RestAssured.given()
                .header("Authorization", "Bearer $accessToken")
                .`when`()
                .get("/api/poses")
                .then()
                .statusCode(HttpStatus.OK.value())
                .body("resultCode", equalTo(ResultCode.SUCCESS.code))
                .body("data.items", hasSize<Int>(20))
                .body("data.hasNext", equalTo(true))
        }
    }

    @Nested
    @DisplayName("정렬 테스트")
    inner class SortingTests {

        @Test
        @DisplayName("포즈 목록 정렬 - 최신순(DESC) 기본값")
        fun givenMultiplePoses_whenGetWithDefaultSort_thenReturnsDescOrder() {
            // given: 순서대로 3개의 포즈 생성 (생성 시간 차이 확보)
            val poses = (1..3).map {
                val media = createMedia(ownerId = testUser.id!!, status = MediaStatus.UPLOADED)
                Thread.sleep(10)
                createPose(userId = testUser.id!!, mediaId = media.id!!)
            }

            // when & then: 기본 정렬(DESC)로 조회 시 최신 포즈가 먼저 나옴
            RestAssured.given()
                .header("Authorization", "Bearer $accessToken")
                .`when`()
                .get("/api/poses")
                .then()
                .statusCode(HttpStatus.OK.value())
                .body("resultCode", equalTo(ResultCode.SUCCESS.code))
                .body("data.items", hasSize<Int>(3))
                .body("data.items[0].poseId", equalTo(poses[2].id!!.toInt()))
                .body("data.items[1].poseId", equalTo(poses[1].id!!.toInt()))
                .body("data.items[2].poseId", equalTo(poses[0].id!!.toInt()))
        }

        @Test
        @DisplayName("포즈 목록 정렬 - 오래된순(ASC)")
        fun givenMultiplePoses_whenGetWithAscSort_thenReturnsAscOrder() {
            // given: 순서대로 3개의 포즈 생성
            val poses = (1..3).map {
                val media = createMedia(ownerId = testUser.id!!, status = MediaStatus.UPLOADED)
                Thread.sleep(10)
                createPose(userId = testUser.id!!, mediaId = media.id!!)
            }

            // when & then: ASC 정렬로 조회 시 오래된 포즈가 먼저 나옴
            RestAssured.given()
                .header("Authorization", "Bearer $accessToken")
                .queryParam("sortOrder", "ASC")
                .`when`()
                .get("/api/poses")
                .then()
                .statusCode(HttpStatus.OK.value())
                .body("resultCode", equalTo(ResultCode.SUCCESS.code))
                .body("data.items", hasSize<Int>(3))
                .body("data.items[0].poseId", equalTo(poses[0].id!!.toInt()))
                .body("data.items[1].poseId", equalTo(poses[1].id!!.toInt()))
                .body("data.items[2].poseId", equalTo(poses[2].id!!.toInt()))
        }

        @Test
        @DisplayName("포즈 목록 정렬과 페이징 조합 - DESC + 첫 페이지")
        fun givenMultiplePoses_whenGetFirstPageWithDescSort_thenReturnsPagedDescOrder() {
            // given: 5개의 포즈 생성
            val poses = (1..5).map {
                val media = createMedia(ownerId = testUser.id!!, status = MediaStatus.UPLOADED)
                Thread.sleep(10)
                createPose(userId = testUser.id!!, mediaId = media.id!!)
            }

            // when & then: page=0, size=2, DESC 조회 시 최신 2개 반환
            RestAssured.given()
                .header("Authorization", "Bearer $accessToken")
                .queryParam("page", 0)
                .queryParam("size", 2)
                .queryParam("sortOrder", "DESC")
                .`when`()
                .get("/api/poses")
                .then()
                .statusCode(HttpStatus.OK.value())
                .body("resultCode", equalTo(ResultCode.SUCCESS.code))
                .body("data.items", hasSize<Int>(2))
                .body("data.hasNext", equalTo(true))
                .body("data.items[0].poseId", equalTo(poses[4].id!!.toInt()))
                .body("data.items[1].poseId", equalTo(poses[3].id!!.toInt()))
        }
    }

    @Nested
    @DisplayName("실패 케이스")
    inner class FailureTests {

        @Test
        @DisplayName("포즈 목록 조회 실패 - 인증 없이 요청")
        fun givenNoAuth_whenGetPoses_thenReturnsForbidden() {
            // when & then
            RestAssured.given()
                .`when`()
                .get("/api/poses")
                .then()
                .statusCode(HttpStatus.FORBIDDEN.value())
        }
    }
}
