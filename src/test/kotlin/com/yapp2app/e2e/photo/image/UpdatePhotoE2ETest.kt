package com.yapp2app.e2e.photo.image

import com.yapp2app.common.api.dto.ResultCode
import com.yapp2app.media.domain.entity.MediaStatus
import com.yapp2app.photo.api.dto.UpdatePhotoRequest
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
 * fileName       : UpdatePhotoE2ETest
 * author         : koo
 * date           : 2026. 1. 9. 오후 5:26
 * description    : PATCH /api/photos/{photoId} E2E 테스트
 */
@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class UpdatePhotoE2ETest : PhotoImageE2ETestBase() {

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
    @DisplayName("사진 메모 수정 성공")
    fun givenValidMemo_whenUpdatePhoto_thenReturnsSuccess() {
        // given
        val media = createMedia(ownerId = testUser.id!!, status = MediaStatus.UPLOADED)
        val photo = createPhotoImage(userId = testUser.id!!, mediaId = media.id!!)

        // when & then
        RestAssured.given()
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer $accessToken")
            .body(UpdatePhotoRequest("new memo"))
            .`when`()
            .patch("/api/photos/${photo.id}")
            .then()
            .statusCode(HttpStatus.OK.value())
            .body("success", equalTo(true))
            .body("resultCode", equalTo(ResultCode.SUCCESS.code))
    }

    @Test
    @DisplayName("수정하는 메모가 null인 경우 성공한다")
    fun givenNullMemo_whenUpdatePhoto_thenReturnSuccess() {
        // given
        val media = createMedia(ownerId = testUser.id!!, status = MediaStatus.UPLOADED)
        val photo = createPhotoImage(userId = testUser.id!!, mediaId = media.id!!)

        // when & then
        RestAssured.given()
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer $accessToken")
            .body(UpdatePhotoRequest(null))
            .`when`()
            .patch("/api/photos/${photo.id}")
            .then()
            .statusCode(HttpStatus.OK.value())
            .body("success", equalTo(true))
            .body("resultCode", equalTo(ResultCode.SUCCESS.code))
    }

    @Test
    @DisplayName("수정하는 메모가 blank인 경우 성공한다")
    fun givenBlankMemo_whenUpdatePhoto_thenReturnSuccess() {
        // given
        val media = createMedia(ownerId = testUser.id!!, status = MediaStatus.UPLOADED)
        val photo = createPhotoImage(userId = testUser.id!!, mediaId = media.id!!)

        // when & then
        RestAssured.given()
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer $accessToken")
            .body(UpdatePhotoRequest(""))
            .`when`()
            .patch("/api/photos/${photo.id}")
            .then()
            .statusCode(HttpStatus.OK.value())
            .body("success", equalTo(true))
            .body("resultCode", equalTo(ResultCode.SUCCESS.code))
    }
}
