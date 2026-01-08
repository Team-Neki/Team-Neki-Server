package com.yapp2app.e2e.photo.image

import com.yapp2app.common.api.dto.ResultCode
import com.yapp2app.media.domain.entity.MediaStatus
import com.yapp2app.photo.api.dto.UploadPhotoRequest
import com.yapp2app.user.domain.entity.User
import io.restassured.RestAssured
import io.restassured.http.ContentType
import org.hamcrest.Matchers.equalTo
import org.hamcrest.Matchers.notNullValue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.http.HttpStatus
import org.springframework.test.context.ActiveProfiles

/**
 * fileName       : UploadPhotoE2ETest
 * author         : koo
 * date           : 2026. 1. 8. 오후 7:43
 * description    : POST /api/photos E2E 테스트
 */
@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class UploadPhotoE2ETest : PhotoImageE2ETestBase() {

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
    @DisplayName("사진 업로드 성공 - 폴더 없이 업로드")
    fun givenValidMediaId_whenUploadPhoto_thenReturnsSuccess() {
        // given
        val media = createMedia(ownerId = testUser.id!!, status = MediaStatus.UPLOADED)

        // when & then
        RestAssured.given()
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer $accessToken")
            .body(UploadPhotoRequest(media.id, null))
            .`when`()
            .post("/api/photos")
            .then()
            .statusCode(HttpStatus.OK.value())
            .body("success", equalTo(true))
            .body("resultCode", equalTo(ResultCode.SUCCESS.code))
            .body("data.photoId", notNullValue())
    }

    @Test
    @DisplayName("사진 업로드 성공 - 폴더와 함께 업로드")
    fun givenValidMediaIdAndFolderId_whenUploadPhoto_thenReturnsSuccess() {
        // given
        val media = createMedia(ownerId = testUser.id!!, status = MediaStatus.UPLOADED)
        val folder = createFolder(userId = testUser.id!!)

        // when & then
        RestAssured.given()
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer $accessToken")
            .body(UploadPhotoRequest(media.id, folder.id))
            .`when`()
            .post("/api/photos")
            .then()
            .statusCode(HttpStatus.OK.value())
            .body("success", equalTo(true))
            .body("resultCode", equalTo(ResultCode.SUCCESS.code))
            .body("data.photoId", notNullValue())
    }

    @Test
    @DisplayName("mediaId가 누락된 경우 400 에러를 반환한다")
    fun givenMissingMediaId_whenUploadPhoto_thenReturnsBadRequest() {
        // when & then
        RestAssured.given()
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer $accessToken")
            .body(UploadPhotoRequest(null, null))
            .`when`()
            .post("/api/photos")
            .then()
            .statusCode(HttpStatus.BAD_REQUEST.value())
            .body("success", equalTo(false))
            .body("resultCode", equalTo(ResultCode.INVALID_PARAMETER.code))
    }

    @Test
    @DisplayName("미디어가 업로드되지 않은 경우 NOT_FOUND 에러를 반환한다")
    fun givenMediaNotUploaded_whenUploadPhoto_thenReturnsNotFound() {
        // given
        val media = createMedia(ownerId = testUser.id!!, status = MediaStatus.INITIATED)

        // when & then
        RestAssured.given()
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer $accessToken")
            .body(UploadPhotoRequest(media.id, null))
            .`when`()
            .post("/api/photos")
            .then()
            .statusCode(HttpStatus.BAD_REQUEST.value())
            .body("success", equalTo(false))
            .body("resultCode", equalTo(ResultCode.NOT_FOUND.code))
    }

    @Test
    @DisplayName("다른 사용자의 미디어로 업로드 시 NOT_FOUND 에러를 반환한다")
    fun givenOtherUserMedia_whenUploadPhoto_thenReturnsNotFound() {
        // given
        val (otherUser, _) = createTestUserAndToken(email = "other@example.com")
        val media = createMedia(ownerId = otherUser.id!!, status = MediaStatus.UPLOADED)

        // when & then
        RestAssured.given()
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer $accessToken")
            .body(UploadPhotoRequest(media.id, null))
            .`when`()
            .post("/api/photos")
            .then()
            .statusCode(HttpStatus.BAD_REQUEST.value())
            .body("success", equalTo(false))
            .body("resultCode", equalTo(ResultCode.NOT_FOUND.code))
    }

    @Test
    @DisplayName("인증되지 않은 사용자가 업로드 시도 시 401 에러를 반환한다")
    fun givenNoAuthToken_whenUploadPhoto_thenReturnsUnauthorized() {
        // when & then
        RestAssured.given()
            .contentType(ContentType.JSON)
            .body(UploadPhotoRequest(1, null))
            .`when`()
            .post("/api/photos")
            .then()
            .statusCode(HttpStatus.FORBIDDEN.value())
    }

    @Test
    @DisplayName("존재하지 않는 미디어로 업로드 시 NOT_FOUND 에러를 반환한다")
    fun givenNonExistentMediaId_whenUploadPhoto_thenReturnsNotFound() {
        // when & then
        RestAssured.given()
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer $accessToken")
            .body(UploadPhotoRequest(999999, null))
            .`when`()
            .post("/api/photos")
            .then()
            .statusCode(HttpStatus.BAD_REQUEST.value())
            .body("success", equalTo(false))
            .body("resultCode", equalTo(ResultCode.NOT_FOUND.code))
    }
}
