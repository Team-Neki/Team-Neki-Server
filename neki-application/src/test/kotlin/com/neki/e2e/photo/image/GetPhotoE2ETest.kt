package com.neki.e2e.photo.image

import com.neki.common.api.dto.ResultCode
import com.neki.media.entity.MediaStatus
import com.neki.user.entity.User
import io.restassured.RestAssured
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
 * fileName       : GetPhotoE2ETest
 * author         : claude
 * date           : 2026. 1. 26.
 * description    : GET /api/photos/{photoId} E2E 테스트
 */
@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class GetPhotoE2ETest : PhotoImageE2ETestBase() {

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
        @DisplayName("사진 상세 조회 성공 - 기본 조회")
        fun givenPhotoExists_whenGetPhoto_thenReturnsPhotoDetail() {
            // given
            val media = createMedia(ownerId = testUser.id!!, status = MediaStatus.UPLOADED)
            val photo = createPhotoImage(userId = testUser.id!!, mediaId = media.id!!)

            // when & then
            RestAssured.given()
                .header("Authorization", "Bearer $accessToken")
                .`when`()
                .get("/api/photos/${photo.id}")
                .then()
                .statusCode(HttpStatus.OK.value())
                .body("resultCode", equalTo(ResultCode.SUCCESS.code))
                .body("data.photoId", equalTo(photo.id!!.toInt()))
                .body("data.imageUrl", startsWith("/file/image/"))
                .body("data.favorite", equalTo(false))
                .body("data.contentType", equalTo("image/jpeg"))
                .body("data.createdAt", notNullValue())
        }

        @Test
        @DisplayName("사진 상세 조회 성공 - 즐겨찾기된 사진")
        fun givenFavoritePhoto_whenGetPhoto_thenReturnsFavoriteTrue() {
            // given
            val media = createMedia(ownerId = testUser.id!!, status = MediaStatus.UPLOADED)
            val photo = createFavoritePhotoImage(userId = testUser.id!!, mediaId = media.id!!)

            // when & then
            RestAssured.given()
                .header("Authorization", "Bearer $accessToken")
                .`when`()
                .get("/api/photos/${photo.id}")
                .then()
                .statusCode(HttpStatus.OK.value())
                .body("resultCode", equalTo(ResultCode.SUCCESS.code))
                .body("data.photoId", equalTo(photo.id!!.toInt()))
                .body("data.favorite", equalTo(true))
        }

        @Test
        @DisplayName("사진 상세 조회 성공 - 폴더에 속한 사진")
        fun givenPhotoInFolder_whenGetPhoto_thenReturnsPhotoDetail() {
            // given
            val folder = createFolder(userId = testUser.id!!, name = "테스트 폴더")
            val media = createMedia(ownerId = testUser.id!!, status = MediaStatus.UPLOADED)
            val photo = createPhotoImage(userId = testUser.id!!, mediaId = media.id!!, folderId = folder.id)

            // when & then
            RestAssured.given()
                .header("Authorization", "Bearer $accessToken")
                .`when`()
                .get("/api/photos/${photo.id}")
                .then()
                .statusCode(HttpStatus.OK.value())
                .body("resultCode", equalTo(ResultCode.SUCCESS.code))
                .body("data.photoId", equalTo(photo.id!!.toInt()))
                .body("data.imageUrl", startsWith("/file/image/"))
        }
    }

    @Nested
    @DisplayName("실패 케이스")
    inner class FailureTests {

        @Test
        @DisplayName("사진 상세 조회 실패 - 존재하지 않는 사진")
        fun givenNonExistentPhoto_whenGetPhoto_thenReturnsNotFound() {
            // given
            val nonExistentPhotoId = 999999L

            // when & then
            RestAssured.given()
                .header("Authorization", "Bearer $accessToken")
                .`when`()
                .get("/api/photos/$nonExistentPhotoId")
                .then()
                .statusCode(HttpStatus.BAD_REQUEST.value())
                .body("resultCode", equalTo(ResultCode.NOT_FOUND.code))
        }

        @Test
        @DisplayName("사진 상세 조회 실패 - 다른 사용자의 사진")
        fun givenOtherUserPhoto_whenGetPhoto_thenReturnsNotFound() {
            // given
            val (otherUser, _) = createTestUserAndToken(email = "other@example.com")
            val media = createMedia(ownerId = otherUser.id!!, status = MediaStatus.UPLOADED)
            val otherUserPhoto = createPhotoImage(userId = otherUser.id!!, mediaId = media.id!!)

            // when & then
            RestAssured.given()
                .header("Authorization", "Bearer $accessToken")
                .`when`()
                .get("/api/photos/${otherUserPhoto.id}")
                .then()
                .statusCode(HttpStatus.BAD_REQUEST.value())
                .body("resultCode", equalTo(ResultCode.NOT_FOUND.code))
        }

        @Test
        @DisplayName("사진 상세 조회 실패 - 인증 없이 요청")
        fun givenNoAuth_whenGetPhoto_thenReturnsUnauthorized() {
            // given
            val media = createMedia(ownerId = testUser.id!!, status = MediaStatus.UPLOADED)
            val photo = createPhotoImage(userId = testUser.id!!, mediaId = media.id!!)

            // when & then
            RestAssured.given()
                .`when`()
                .get("/api/photos/${photo.id}")
                .then()
                .statusCode(HttpStatus.FORBIDDEN.value())
        }
    }
}
