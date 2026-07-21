package com.neki.e2e.photo.image

import com.neki.common.code.ResultCode
import com.neki.media.entity.MediaStatus
import com.neki.photo.api.dto.UpdatePhotoFavoriteRequest
import com.neki.photo.entity.FavoritePhotoId
import com.neki.user.entity.User
import io.restassured.RestAssured
import io.restassured.http.ContentType
import org.assertj.core.api.Assertions.assertThat
import org.hamcrest.Matchers.equalTo
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.data.repository.findByIdOrNull
import org.springframework.http.HttpStatus
import org.springframework.test.context.ActiveProfiles

/**
 * fileName       : UpdatePhotoFavoriteE2ETest
 * author         : koo
 * date           : 2026. 1. 14. 오전 12:24
 * description    :
 */
@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class UpdatePhotoFavoriteE2ETest : PhotoImageE2ETestBase() {

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
    @DisplayName("사진 즐겨찾기 성공")
    fun givenPhotoImage_whenFavorite_thenReturnSuccess() {
        // given
        val media = createMedia(ownerId = testUser.id!!, status = MediaStatus.UPLOADED)
        val photo = createPhotoImage(userId = testUser.id!!, mediaId = media.id!!)

        // when & then
        RestAssured.given()
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer $accessToken")
            .body(UpdatePhotoFavoriteRequest(true))
            .`when`()
            .patch("/api/photos/${photo.id}/favorite")
            .then()
            .statusCode(HttpStatus.OK.value())
            .body("resultCode", equalTo(ResultCode.SUCCESS.code))

        val favoritePhoto =
            favoritePhotoRepository.findByIdOrNull(
                FavoritePhotoId(testUser.id!!, photo.id!!),
            )

        assertThat(favoritePhoto).isNotNull()
    }

    @Test
    @DisplayName("여러 번 즐겨찾기를 등록해도 성공")
    fun givenPhotoImage_whenFavoriteTwice_thenReturnSuccess() {
        // given
        val media = createMedia(ownerId = testUser.id!!, status = MediaStatus.UPLOADED)
        val photo = createPhotoImage(userId = testUser.id!!, mediaId = media.id!!)

        // when & then
        RestAssured.given()
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer $accessToken")
            .body(UpdatePhotoFavoriteRequest(true))
            .`when`()
            .patch("/api/photos/${photo.id}/favorite")
            .then()
            .statusCode(HttpStatus.OK.value())
            .body("resultCode", equalTo(ResultCode.SUCCESS.code))

        RestAssured.given()
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer $accessToken")
            .body(UpdatePhotoFavoriteRequest(true))
            .`when`()
            .patch("/api/photos/${photo.id}/favorite")
            .then()
            .statusCode(HttpStatus.OK.value())
            .body("resultCode", equalTo(ResultCode.SUCCESS.code))

        val favoritePhoto =
            favoritePhotoRepository.findByIdOrNull(
                FavoritePhotoId(testUser.id!!, photo.id!!),
            )

        assertThat(favoritePhoto).isNotNull()
    }

    @Test
    @DisplayName("즐겨찾기하지 않은 사진을 즐겨찾기 삭제하는 경우 성공(아무 동작도 하지 않음)")
    fun givenPhotoImage_whenUnmarkFavorite_thenSuccess() {
        // given
        val media = createMedia(ownerId = testUser.id!!, status = MediaStatus.UPLOADED)
        val photo = createPhotoImage(userId = testUser.id!!, mediaId = media.id!!)

        // when & then
        RestAssured.given()
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer $accessToken")
            .body(UpdatePhotoFavoriteRequest(false))
            .`when`()
            .patch("/api/photos/${photo.id}/favorite")
            .then()
            .statusCode(HttpStatus.OK.value())
            .body("resultCode", equalTo(ResultCode.SUCCESS.code))

        val favoritePhoto =
            favoritePhotoRepository.findByIdOrNull(
                FavoritePhotoId(testUser.id!!, photo.id!!),
            )

        assertThat(favoritePhoto).isNull()
    }

    @Test
    @DisplayName("즐겨찾기한 사진을 즐겨찾기에서 삭제하면 성공")
    fun givenFavoritePhotoImage_whenUnmarkFavorite_thenSuccess() {
        // given
        val media = createMedia(ownerId = testUser.id!!, status = MediaStatus.UPLOADED)
        val photo = createPhotoImage(userId = testUser.id!!, mediaId = media.id!!)

        RestAssured.given()
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer $accessToken")
            .body(UpdatePhotoFavoriteRequest(true))
            .`when`()
            .patch("/api/photos/${photo.id}/favorite")
            .then()
            .statusCode(HttpStatus.OK.value())
            .body("resultCode", equalTo(ResultCode.SUCCESS.code))

        // when & then
        RestAssured.given()
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer $accessToken")
            .body(UpdatePhotoFavoriteRequest(false))
            .`when`()
            .patch("/api/photos/${photo.id}/favorite")
            .then()
            .statusCode(HttpStatus.OK.value())
            .body("resultCode", equalTo(ResultCode.SUCCESS.code))

        val favoritePhoto =
            favoritePhotoRepository.findByIdOrNull(
                FavoritePhotoId(testUser.id!!, photo.id!!),
            )

        assertThat(favoritePhoto).isNull()
    }

    @Test
    @DisplayName("즐겨찾기한 사진을 여러 번 즐겨찾기에서 삭제해도 성공")
    fun givenFavoritePhotoImage_whenUnmarkFavoriteTwice_thenSuccess() {
        // given
        val media = createMedia(ownerId = testUser.id!!, status = MediaStatus.UPLOADED)
        val photo = createPhotoImage(userId = testUser.id!!, mediaId = media.id!!)

        RestAssured.given()
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer $accessToken")
            .body(UpdatePhotoFavoriteRequest(true))
            .`when`()
            .patch("/api/photos/${photo.id}/favorite")
            .then()
            .statusCode(HttpStatus.OK.value())
            .body("resultCode", equalTo(ResultCode.SUCCESS.code))

        // when & then
        RestAssured.given()
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer $accessToken")
            .body(UpdatePhotoFavoriteRequest(false))
            .`when`()
            .patch("/api/photos/${photo.id}/favorite")
            .then()
            .statusCode(HttpStatus.OK.value())
            .body("resultCode", equalTo(ResultCode.SUCCESS.code))

        RestAssured.given()
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer $accessToken")
            .body(UpdatePhotoFavoriteRequest(false))
            .`when`()
            .patch("/api/photos/${photo.id}/favorite")
            .then()
            .statusCode(HttpStatus.OK.value())
            .body("resultCode", equalTo(ResultCode.SUCCESS.code))

        val favoritePhoto =
            favoritePhotoRepository.findByIdOrNull(
                FavoritePhotoId(testUser.id!!, photo.id!!),
            )

        assertThat(favoritePhoto).isNull()
    }
}
