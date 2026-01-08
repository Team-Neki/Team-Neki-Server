package com.yapp2app.e2e.photo.image

import com.yapp2app.common.api.dto.ResultCode
import com.yapp2app.media.domain.entity.MediaStatus
import com.yapp2app.user.domain.entity.User
import io.restassured.RestAssured
import org.hamcrest.Matchers.equalTo
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.http.HttpStatus
import org.springframework.test.context.ActiveProfiles

/**
 * fileName       : DeletePhotoE2ETest
 * author         : koo
 * date           : 2026. 1. 8.
 * description    : DELETE /api/photos/{photoImageId} E2E 테스트
 */
@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class DeletePhotoE2ETest : PhotoImageE2ETestBase() {

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
    @DisplayName("사진 삭제 성공")
    fun givenValidPhotoId_whenDeletePhoto_thenReturnsSuccess() {
        // given
        val media = createMedia(ownerId = testUser.id!!, status = MediaStatus.UPLOADED)
        val photo = createPhotoImage(userId = testUser.id!!, mediaId = media.id!!)

        // when & then
        RestAssured.given()
            .header("Authorization", "Bearer $accessToken")
            .`when`()
            .delete("/api/photos/${photo.id}")
            .then()
            .statusCode(HttpStatus.OK.value())
            .body("success", equalTo(true))
            .body("resultCode", equalTo(ResultCode.SUCCESS.code))
    }

    @Test
    @DisplayName("존재하지 않는 사진 삭제 시 NOT_FOUND 에러를 반환한다")
    fun givenNonExistentPhotoId_whenDeletePhoto_thenReturnsNotFound() {
        // given
        val nonExistentPhotoId = 999999L

        // when & then
        RestAssured.given()
            .header("Authorization", "Bearer $accessToken")
            .`when`()
            .delete("/api/photos/$nonExistentPhotoId")
            .then()
            .statusCode(HttpStatus.BAD_REQUEST.value())
            .body("success", equalTo(false))
            .body("code", equalTo(ResultCode.NOT_FOUND.code))
    }

    @Test
    @DisplayName("다른 사용자의 사진 삭제 시도 시 NOT_FOUND 에러를 반환한다")
    fun givenOtherUserPhoto_whenDeletePhoto_thenReturnsNotFound() {
        // given
        val (otherUser, _) = createTestUserAndToken(email = "other@example.com")
        val media = createMedia(ownerId = otherUser.id!!, status = MediaStatus.UPLOADED)
        val otherUserPhoto = createPhotoImage(userId = otherUser.id, mediaId = media.id!!)

        // when & then
        RestAssured.given()
            .header("Authorization", "Bearer $accessToken")
            .`when`()
            .delete("/api/photos/${otherUserPhoto.id}")
            .then()
            .statusCode(HttpStatus.BAD_REQUEST.value())
            .body("success", equalTo(false))
            .body("code", equalTo(ResultCode.NOT_FOUND.code))
    }

    @Test
    @DisplayName("인증되지 않은 사용자가 삭제 시도 시 401 에러를 반환한다")
    fun givenNoAuthToken_whenDeletePhoto_thenReturnsUnauthorized() {
        // given
        val media = createMedia(ownerId = testUser.id!!, status = MediaStatus.UPLOADED)
        val photo = createPhotoImage(userId = testUser.id!!, mediaId = media.id!!)

        // when & then
        RestAssured.given()
            .`when`()
            .delete("/api/photos/${photo.id}")
            .then()
            .statusCode(HttpStatus.FORBIDDEN.value())
    }

    @Test
    @DisplayName("폴더에 포함된 사진 삭제 성공")
    fun givenPhotoInFolder_whenDeletePhoto_thenReturnsSuccess() {
        // given
        val folder = createFolder(userId = testUser.id!!)
        val media = createMedia(ownerId = testUser.id!!, status = MediaStatus.UPLOADED)
        val photo = createPhotoImage(userId = testUser.id!!, mediaId = media.id!!, folderId = folder.id)

        // when & then
        RestAssured.given()
            .header("Authorization", "Bearer $accessToken")
            .`when`()
            .delete("/api/photos/${photo.id}")
            .then()
            .statusCode(HttpStatus.OK.value())
            .body("success", equalTo(true))
            .body("resultCode", equalTo(ResultCode.SUCCESS.code))
    }
}
