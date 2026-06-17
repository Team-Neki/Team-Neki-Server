package com.neki.e2e.photo.image

import com.neki.common.api.dto.ResultCode
import com.neki.media.entity.MediaStatus
import com.neki.photo.api.dto.DeletePhotosRequest
import com.neki.user.entity.User
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
 * description    : DELETE /api/photos E2E 테스트 (단건 삭제 및 다건 삭제)
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
    @DisplayName("단건 삭제 성공")
    fun givenSinglePhotoId_whenDeletePhotos_thenReturnsSuccess() {
        // given
        val media = createMedia(ownerId = testUser.id!!, status = MediaStatus.UPLOADED)
        val photo = createPhotoImage(userId = testUser.id!!, mediaId = media.id!!)

        // when & then
        RestAssured.given()
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer $accessToken")
            .body(DeletePhotosRequest(photoIds = listOf(photo.id!!)))
            .`when`()
            .delete("/api/photos")
            .then()
            .statusCode(HttpStatus.OK.value())
            .body("resultCode", equalTo(ResultCode.SUCCESS.code))
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
            .body("resultCode", equalTo(ResultCode.INVALID_PARAMETER.code))
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
            .body("resultCode", equalTo(ResultCode.INVALID_PARAMETER.code))
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
        val otherPhoto = createPhotoImage(userId = otherUser.id!!, mediaId = otherMedia.id!!)

        // when & then
        RestAssured.given()
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer $accessToken")
            .body(DeletePhotosRequest(photoIds = listOf(myPhoto.id!!, otherPhoto.id!!)))
            .`when`()
            .delete("/api/photos")
            .then()
            .statusCode(HttpStatus.OK.value())
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
            .body("resultCode", equalTo(ResultCode.SUCCESS.code))
    }

    @Test
    @DisplayName("존재하지 않는 사진 ID만 있을 때 성공을 반환한다")
    fun givenNonExistentPhotoIdOnly_whenDeletePhotos_thenReturnsSuccess() {
        // given
        val nonExistentPhotoId = 999999L

        // when & then
        RestAssured.given()
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer $accessToken")
            .body(DeletePhotosRequest(photoIds = listOf(nonExistentPhotoId)))
            .`when`()
            .delete("/api/photos")
            .then()
            .statusCode(HttpStatus.OK.value())
            .body("resultCode", equalTo(ResultCode.SUCCESS.code))
    }

    @Test
    @DisplayName("다른 사용자의 사진 ID만 있을 때 성공을 반환한다")
    fun givenOtherUserPhotoIdOnly_whenDeletePhotos_thenReturnsSuccess() {
        // given
        val (otherUser, _) = createTestUserAndToken(email = "other2@example.com")
        val media = createMedia(ownerId = otherUser.id!!, status = MediaStatus.UPLOADED)
        val otherUserPhoto = createPhotoImage(userId = otherUser.id!!, mediaId = media.id!!)

        // when & then
        RestAssured.given()
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer $accessToken")
            .body(DeletePhotosRequest(photoIds = listOf(otherUserPhoto.id!!)))
            .`when`()
            .delete("/api/photos")
            .then()
            .statusCode(HttpStatus.OK.value())
            .body("resultCode", equalTo(ResultCode.SUCCESS.code))
    }

    @Test
    @DisplayName("폴더에 포함된 사진 단건 삭제 성공")
    fun givenPhotoInFolder_whenDeletePhotos_thenReturnsSuccess() {
        // given
        val folder = createFolder(userId = testUser.id!!)
        val media = createMedia(ownerId = testUser.id!!, status = MediaStatus.UPLOADED)
        val photo = createPhotoImage(userId = testUser.id!!, mediaId = media.id!!, folderId = folder.id)

        // when & then
        RestAssured.given()
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer $accessToken")
            .body(DeletePhotosRequest(photoIds = listOf(photo.id!!)))
            .`when`()
            .delete("/api/photos")
            .then()
            .statusCode(HttpStatus.OK.value())
            .body("resultCode", equalTo(ResultCode.SUCCESS.code))
    }

    @Test
    @DisplayName("즐겨찾기된 사진 삭제 시 즐겨찾기도 함께 삭제된다")
    fun givenFavoritePhoto_whenDeletePhotos_thenFavoriteAlsoDeleted() {
        // given
        val media = createMedia(ownerId = testUser.id!!, status = MediaStatus.UPLOADED)
        val photo = createFavoritePhotoImage(userId = testUser.id!!, mediaId = media.id!!)

        // when
        RestAssured.given()
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer $accessToken")
            .body(DeletePhotosRequest(photoIds = listOf(photo.id!!)))
            .`when`()
            .delete("/api/photos")
            .then()
            .statusCode(HttpStatus.OK.value())

        // then
        RestAssured.given()
            .header("Authorization", "Bearer $accessToken")
            .`when`()
            .get("/api/photos/favorite/summary")
            .then()
            .statusCode(HttpStatus.OK.value())
            .body("data.totalCount", equalTo(0))
    }
}
