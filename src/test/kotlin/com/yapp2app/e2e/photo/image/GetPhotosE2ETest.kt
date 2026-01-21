package com.yapp2app.e2e.photo.image

import com.yapp2app.common.api.dto.ResultCode
import com.yapp2app.media.domain.entity.MediaStatus
import com.yapp2app.user.domain.entity.User
import io.restassured.RestAssured
import org.hamcrest.Matchers.empty
import org.hamcrest.Matchers.equalTo
import org.hamcrest.Matchers.hasSize
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.http.HttpStatus
import org.springframework.test.context.ActiveProfiles

/**
 * fileName       : GetPhotosE2ETest
 * author         : koo
 * date           : 2026. 1. 8.
 * description    : GET /api/photos E2E 테스트
 */
@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class GetPhotosE2ETest : PhotoImageE2ETestBase() {

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
    @DisplayName("사진 목록 조회 성공 - 전체 사진 조회")
    fun givenPhotosExist_whenGetPhotos_thenReturnsPhotoList() {
        // given
        val media1 = createMedia(ownerId = testUser.id!!, status = MediaStatus.UPLOADED)
        val media2 = createMedia(ownerId = testUser.id!!, status = MediaStatus.UPLOADED)
        createPhotoImage(userId = testUser.id!!, mediaId = media1.id!!)
        createPhotoImage(userId = testUser.id!!, mediaId = media2.id!!)

        // when & then
        RestAssured.given()
            .header("Authorization", "Bearer $accessToken")
            .`when`()
            .get("/api/photos")
            .then()
            .statusCode(HttpStatus.OK.value())
            .body("resultCode", equalTo(ResultCode.SUCCESS.code))
            .body("data.items", hasSize<Int>(2))
    }

    @Test
    @DisplayName("사진 목록 조회 성공 - 폴더별 사진 조회")
    fun givenPhotosInFolder_whenGetPhotosByFolderId_thenReturnsFilteredPhotos() {
        // given
        val folder = createFolder(userId = testUser.id!!)
        val media1 = createMedia(ownerId = testUser.id!!, status = MediaStatus.UPLOADED)
        val media2 = createMedia(ownerId = testUser.id!!, status = MediaStatus.UPLOADED)
        val media3 = createMedia(ownerId = testUser.id!!, status = MediaStatus.UPLOADED)

        createPhotoImage(userId = testUser.id!!, mediaId = media1.id!!, folderId = folder.id)
        createPhotoImage(userId = testUser.id!!, mediaId = media2.id!!, folderId = folder.id)
        createPhotoImage(userId = testUser.id!!, mediaId = media3.id!!, folderId = null)

        // when & then
        RestAssured.given()
            .header("Authorization", "Bearer $accessToken")
            .queryParam("folderId", folder.id)
            .`when`()
            .get("/api/photos")
            .then()
            .statusCode(HttpStatus.OK.value())
            .body("resultCode", equalTo(ResultCode.SUCCESS.code))
            .body("data.items", hasSize<Int>(2))
    }

    @Test
    @DisplayName("사진 목록 조회 성공 - 사진이 없는 경우 빈 목록 반환")
    fun givenNoPhotos_whenGetPhotos_thenReturnsEmptyList() {
        // given - no photos created
        println("accessToken: $accessToken")
        println("testUser.id: ${testUser.id}")

        // when & then
        RestAssured.given()
            .log().all()
            .header("Authorization", "Bearer $accessToken")
            .`when`()
            .get("/api/photos")
            .then()
            .log().all()
            .statusCode(HttpStatus.OK.value())
            .body("resultCode", equalTo(ResultCode.SUCCESS.code))
            .body("data.items", empty<Any>())
    }

    @Test
    @DisplayName("다른 사용자의 사진은 조회되지 않는다")
    fun givenOtherUserPhotos_whenGetPhotos_thenReturnsEmptyList() {
        // given
        val (otherUser, _) = createTestUserAndToken(email = "other@example.com")
        val media = createMedia(ownerId = otherUser.id!!, status = MediaStatus.UPLOADED)
        createPhotoImage(userId = otherUser.id!!, mediaId = media.id!!)

        // when & then
        RestAssured.given()
            .header("Authorization", "Bearer $accessToken")
            .`when`()
            .get("/api/photos")
            .then()
            .statusCode(HttpStatus.OK.value())
            .body("data.items", empty<Any>())
    }

    @Test
    @DisplayName("존재하지 않는 폴더로 조회 시 빈 목록을 반환한다")
    fun givenNonExistentFolderId_whenGetPhotos_thenReturnsEmptyList() {
        // given
        val media = createMedia(ownerId = testUser.id!!, status = MediaStatus.UPLOADED)
        createPhotoImage(userId = testUser.id!!, mediaId = media.id!!, folderId = null)

        // when & then
        RestAssured.given()
            .header("Authorization", "Bearer $accessToken")
            .queryParam("folderId", 999999L)
            .`when`()
            .get("/api/photos")
            .then()
            .statusCode(HttpStatus.OK.value())
            .body("data.items", empty<Any>())
    }
}
