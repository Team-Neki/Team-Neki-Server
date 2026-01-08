package com.yapp2app.e2e.photo.image

import com.yapp2app.common.api.dto.ResultCode
import com.yapp2app.media.domain.entity.MediaStatus
import com.yapp2app.photo.api.dto.DeletePhotosRequest
import com.yapp2app.user.domain.entity.User
import io.restassured.RestAssured
import io.restassured.http.ContentType
import org.hamcrest.Matchers.equalTo
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.http.HttpStatus
import org.springframework.test.context.ActiveProfiles

/**
 * fileName       : DeletePhotosE2ETest
 * author         : koo
 * date           : 2026. 1. 8.
 * description    : DELETE /api/photos (bulk delete) E2E 테스트
 */
@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class DeletePhotosE2ETest : PhotoImageE2ETestBase() {

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
    @DisplayName("여러 사진 일괄 삭제 성공")
    fun givenValidPhotoIds_whenDeletePhotos_thenReturnsSuccess() {
        // given
        val media1 = createMedia(ownerId = testUser.id!!, status = MediaStatus.UPLOADED)
        val media2 = createMedia(ownerId = testUser.id!!, status = MediaStatus.UPLOADED)
        val media3 = createMedia(ownerId = testUser.id!!, status = MediaStatus.UPLOADED)

        val photo1 = createPhotoImage(userId = testUser.id!!, mediaId = media1.id!!)
        val photo2 = createPhotoImage(userId = testUser.id!!, mediaId = media2.id!!)
        val photo3 = createPhotoImage(userId = testUser.id!!, mediaId = media3.id!!)

        // when & then
        RestAssured.given()
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer $accessToken")
            .body(DeletePhotosRequest(photoIds = listOf(photo1.id!!, photo2.id!!, photo3.id!!)))
            .`when`()
            .delete("/api/photos")
            .then()
            .statusCode(HttpStatus.OK.value())
            .body("success", equalTo(true))
            .body("resultCode", equalTo(ResultCode.SUCCESS.code))
    }

    @Test
    @DisplayName("photoIds가 비어있는 경우 400 에러를 반환한다")
    fun givenEmptyPhotoIds_whenDeletePhotos_thenReturnsBadRequest() {
        // when & then
        RestAssured.given()
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer $accessToken")
            .body(DeletePhotosRequest(photoIds = emptyList()))
            .`when`()
            .delete("/api/photos")
            .then()
            .statusCode(HttpStatus.BAD_REQUEST.value())
            .body("success", equalTo(false))
            .body("code", equalTo(ResultCode.INVALID_PARAMETER.code))
    }

    @Test
    @DisplayName("photoIds가 null인 경우 400 에러를 반환한다")
    fun givenNullPhotoIds_whenDeletePhotos_thenReturnsBadRequest() {
        // given
        val requestBody = """
            {
                "photoIds": null
            }
        """.trimIndent()

        // when & then
        RestAssured.given()
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer $accessToken")
            .body(requestBody)
            .`when`()
            .delete("/api/photos")
            .then()
            .statusCode(HttpStatus.BAD_REQUEST.value())
            .body("success", equalTo(false))
            .body("code", equalTo(ResultCode.INVALID_PARAMETER.code))
    }

    @Test
    @DisplayName("인증되지 않은 사용자가 삭제 시도 시 401 에러를 반환한다")
    fun givenNoAuthToken_whenDeletePhotos_thenReturnsUnauthorized() {
        // when & then
        RestAssured.given()
            .contentType(ContentType.JSON)
            .body(DeletePhotosRequest(photoIds = listOf(1, 2, 3)))
            .`when`()
            .delete("/api/photos")
            .then()
            .statusCode(HttpStatus.FORBIDDEN.value())
    }

    @Test
    @DisplayName("존재하지 않는 사진 ID가 포함되어도 나머지 사진은 삭제된다")
    fun givenMixedValidAndInvalidPhotoIds_whenDeletePhotos_thenDeletesValidPhotos() {
        // given
        val media = createMedia(ownerId = testUser.id!!, status = MediaStatus.UPLOADED)
        val photo = createPhotoImage(userId = testUser.id!!, mediaId = media.id!!)
        val notExistPhotoId = 999999L

        // when & then
        RestAssured.given()
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer $accessToken")
            .body(DeletePhotosRequest(photoIds = listOf(photo.id!!, notExistPhotoId)))
            .`when`()
            .delete("/api/photos")
            .then()
            .statusCode(HttpStatus.OK.value())
            .body("success", equalTo(true))
            .body("resultCode", equalTo(ResultCode.SUCCESS.code))
    }

    @Test
    @DisplayName("다른 사용자의 사진 ID가 포함되어도 본인 사진만 삭제된다")
    fun givenOtherUserPhotoIds_whenDeletePhotos_thenDeletesOnlyOwnPhotos() {
        // given
        val (otherUser, _) = createTestUserAndToken(email = "other@example.com")

        val myMedia = createMedia(ownerId = testUser.id!!, status = MediaStatus.UPLOADED)
        val myPhoto = createPhotoImage(userId = testUser.id!!, mediaId = myMedia.id!!)

        val otherMedia = createMedia(ownerId = otherUser.id!!, status = MediaStatus.UPLOADED)
        val otherPhoto = createPhotoImage(userId = otherUser.id, mediaId = otherMedia.id!!)

        // when & then
        RestAssured.given()
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer $accessToken")
            .body(DeletePhotosRequest(photoIds = listOf(myPhoto.id!!, otherPhoto.id!!)))
            .`when`()
            .delete("/api/photos")
            .then()
            .statusCode(HttpStatus.OK.value())
            .body("success", equalTo(true))
            .body("resultCode", equalTo(ResultCode.SUCCESS.code))
    }

    @Test
    @DisplayName("폴더에 포함된 사진들 일괄 삭제 성공")
    fun givenPhotosInFolder_whenDeletePhotos_thenReturnsSuccess() {
        // given
        val folder = createFolder(userId = testUser.id!!)
        val media1 = createMedia(ownerId = testUser.id!!, status = MediaStatus.UPLOADED)
        val media2 = createMedia(ownerId = testUser.id!!, status = MediaStatus.UPLOADED)

        val photo1 = createPhotoImage(userId = testUser.id!!, mediaId = media1.id!!, folderId = folder.id)
        val photo2 = createPhotoImage(userId = testUser.id!!, mediaId = media2.id!!, folderId = folder.id)

        // when & then
        RestAssured.given()
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer $accessToken")
            .body(DeletePhotosRequest(photoIds = listOf(photo1.id!!, photo2.id!!)))
            .`when`()
            .delete("/api/photos")
            .then()
            .statusCode(HttpStatus.OK.value())
            .body("success", equalTo(true))
            .body("resultCode", equalTo(ResultCode.SUCCESS.code))
    }
}
