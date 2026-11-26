package com.neki.api.e2e.share

import com.neki.core.code.ResultCode
import com.neki.domain.media.models.Media
import com.neki.domain.media.models.MediaStatus
import com.neki.domain.photo.models.PhotoImage
import com.neki.domain.share.models.ShareLink
import com.neki.domain.user.models.User
import io.kotest.matchers.longs.shouldBeBetween
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldMatch
import io.kotest.matchers.string.shouldNotContain
import io.restassured.RestAssured
import io.restassured.http.ContentType
import io.restassured.response.ExtractableResponse
import io.restassured.response.Response
import io.restassured.specification.RequestSpecification
import org.hamcrest.Matchers.equalTo
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.http.HttpStatus
import org.springframework.test.context.ActiveProfiles
import java.time.Duration
import java.time.LocalDateTime

/**
 * fileName       : IssueShareLinkE2ETest
 * author         : darren
 * date           : 2026. 11. 26.
 * description    : POST /api/shares E2E 테스트
 */
@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class IssueShareLinkE2ETest : ShareE2ETestBase() {

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

    private fun issue(photoId: Long?, token: String? = accessToken): ExtractableResponse<Response> {
        val spec: RequestSpecification = RestAssured.given().contentType(ContentType.JSON)
        if (token != null) spec.header("Authorization", "Bearer $token")
        val body: Map<String, Any?> = if (photoId == null) emptyMap() else mapOf("photoId" to photoId)
        return spec.body(body).`when`().post("/api/shares").then().extract()
    }

    @Nested
    @DisplayName("성공 케이스")
    inner class SuccessTests {

        @Test
        @DisplayName("소유한 사진을 공유하면 43자 토큰, 공유 웹페이지 url, 약 7일 뒤 만료를 돌려준다")
        fun givenOwnedPhoto_whenIssue_thenReturnsShareLink() {
            // given
            val (photo, _) = createSharablePhoto(testUser.id!!)

            // when
            val response: ExtractableResponse<Response> = issue(photo.id!!)

            // then
            response.statusCode() shouldBe HttpStatus.OK.value()
            response.path<String>("resultCode") shouldBe ResultCode.SUCCESS.code
            val token: String = response.path("data.token")
            val url: String = response.path("data.url")
            val expiresAt: String = response.path("data.expiresAt")
            token.length shouldBe 43
            token shouldMatch Regex("^[A-Za-z0-9_-]+$")
            // test 프로필은 application.yaml 공통 값(web-url, 빈 web-url-query)을 그대로 써서 prod 와 같이 쿼리가 없다
            url shouldBe "https://neki-share.suitestudy.com/s/$token"
            expiresAt shouldMatch Regex("^\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}$")
            val diffSeconds: Long =
                Duration.between(LocalDateTime.now().plusDays(7), LocalDateTime.parse(expiresAt)).seconds
            diffSeconds.shouldBeBetween(-60, 60)
        }

        @Test
        @DisplayName("같은 사진을 다시 공유하면 같은 토큰과 만료를 돌려주고 행이 늘지 않는다")
        fun givenAlreadyShared_whenIssueAgain_thenReusesToken() {
            // given
            val (photo, _) = createSharablePhoto(testUser.id!!)
            val first: ExtractableResponse<Response> = issue(photo.id!!)

            // when
            val second: ExtractableResponse<Response> = issue(photo.id!!)

            // then
            second.path<String>("data.token") shouldBe first.path<String>("data.token")
            second.path<String>("data.expiresAt") shouldBe first.path<String>("data.expiresAt")
            shareLinkRepository.count() shouldBe 1L
        }

        @Test
        @DisplayName("만료된 링크만 있으면 새 토큰을 발급한다")
        fun givenOnlyExpiredLink_whenIssue_thenIssuesNewToken() {
            // given
            val (photo, _) = createSharablePhoto(testUser.id!!)
            val expired: ShareLink =
                saveShareLink(testUser.id!!, photo.id!!, expiresAt = LocalDateTime.now().minusMinutes(1))

            // when
            val response: ExtractableResponse<Response> = issue(photo.id!!)

            // then
            response.statusCode() shouldBe HttpStatus.OK.value()
            response.path<String>("data.token") shouldNotBe expired.token
            shareLinkRepository.count() shouldBe 2L
        }

        @Test
        @DisplayName("soft delete 된 링크만 있으면 새 토큰을 발급한다")
        fun givenOnlyDeletedLink_whenIssue_thenIssuesNewToken() {
            // given
            val (photo, _) = createSharablePhoto(testUser.id!!)
            val deleted: ShareLink =
                saveShareLink(testUser.id!!, photo.id!!, deletedAt = LocalDateTime.now().minusMinutes(1))

            // when
            val response: ExtractableResponse<Response> = issue(photo.id!!)

            // then
            response.statusCode() shouldBe HttpStatus.OK.value()
            response.path<String>("data.token") shouldNotBe deleted.token
        }

        @Test
        @DisplayName("응답 body 에 storageKey 가 없다")
        fun givenOwnedPhoto_whenIssue_thenResponseHasNoStorageKey() {
            // given
            val (photo, media) = createSharablePhoto(testUser.id!!)

            // when
            val response: ExtractableResponse<Response> = issue(photo.id!!)

            // then
            response.body().asString() shouldNotContain media.storageKey
            response.body().asString() shouldNotContain "/file/image"
        }
    }

    @Nested
    @DisplayName("실패 케이스")
    inner class FailureTests {

        @Test
        @DisplayName("타인 사진은 400 D-04")
        fun givenOthersPhoto_whenIssue_thenNotFound() {
            // given
            val (other, _) = createTestUserAndToken(email = "other-${System.nanoTime()}@example.com")
            val (photo, _) = createSharablePhoto(other.id!!)

            // when & then
            issueAndExpect(photo.id!!, HttpStatus.BAD_REQUEST, ResultCode.NOT_FOUND)
            shareLinkRepository.count() shouldBe 0L
        }

        @Test
        @DisplayName("없는 사진은 400 D-04")
        fun givenMissingPhoto_whenIssue_thenNotFound() {
            issueAndExpect(999_999L, HttpStatus.BAD_REQUEST, ResultCode.NOT_FOUND)
        }

        @Test
        @DisplayName("삭제된 사진은 400 D-04")
        fun givenDeletedPhoto_whenIssue_thenNotFound() {
            // given
            val (photo, _) = createSharablePhoto(testUser.id!!)
            photo.softDelete()
            photoImageRepository.save(photo)

            // when & then
            issueAndExpect(photo.id!!, HttpStatus.BAD_REQUEST, ResultCode.NOT_FOUND)
        }

        @Test
        @DisplayName("media 가 INITIATED 인 사진은 400 D-04")
        fun givenInitiatedMedia_whenIssue_thenNotFound() {
            // given
            val media: Media = createMedia(ownerId = testUser.id!!, status = MediaStatus.INITIATED)
            val photo: PhotoImage = createPhoto(testUser.id!!, media.id!!)

            // when & then
            issueAndExpect(photo.id!!, HttpStatus.BAD_REQUEST, ResultCode.NOT_FOUND)
        }

        @Test
        @DisplayName("media 가 DELETED 인 사진은 400 D-04")
        fun givenDeletedMedia_whenIssue_thenNotFound() {
            // given
            val media: Media = createMedia(ownerId = testUser.id!!, status = MediaStatus.DELETED)
            val photo: PhotoImage = createPhoto(testUser.id!!, media.id!!)

            // when & then
            issueAndExpect(photo.id!!, HttpStatus.BAD_REQUEST, ResultCode.NOT_FOUND)
        }

        @Test
        @DisplayName("인증이 없으면 기존 entryPoint 대로 403 D-996")
        fun givenNoAuth_whenIssue_thenForbidden() {
            // given
            val (photo, _) = createSharablePhoto(testUser.id!!)

            // when
            val response: ExtractableResponse<Response> = issue(photo.id!!, token = null)

            // then
            response.statusCode() shouldBe HttpStatus.FORBIDDEN.value()
            response.path<String>("resultCode") shouldBe ResultCode.MISSING_TOKEN_ERROR.code
            shareLinkRepository.count() shouldBe 0L
        }

        @Test
        @DisplayName("photoId 가 없으면 400 D-01")
        fun givenMissingPhotoId_whenIssue_thenInvalidParameter() {
            issueAndExpect(null, HttpStatus.BAD_REQUEST, ResultCode.INVALID_PARAMETER)
        }

        @Test
        @DisplayName("photoId 가 음수면 400 D-01")
        fun givenNegativePhotoId_whenIssue_thenInvalidParameter() {
            issueAndExpect(-1L, HttpStatus.BAD_REQUEST, ResultCode.INVALID_PARAMETER)
        }

        private fun issueAndExpect(photoId: Long?, status: HttpStatus, resultCode: ResultCode) {
            val spec: RequestSpecification = RestAssured.given()
                .header("Authorization", "Bearer $accessToken")
                .contentType(ContentType.JSON)
            val body: Map<String, Any?> = if (photoId == null) emptyMap() else mapOf("photoId" to photoId)
            spec.body(body)
                .`when`()
                .post("/api/shares")
                .then()
                .statusCode(status.value())
                .body("resultCode", equalTo(resultCode.code))
        }
    }
}
