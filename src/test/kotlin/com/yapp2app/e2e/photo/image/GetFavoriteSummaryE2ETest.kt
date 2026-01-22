package com.yapp2app.e2e.photo.image

import com.yapp2app.common.api.dto.ResultCode
import com.yapp2app.media.domain.entity.MediaStatus
import com.yapp2app.photo.domain.entity.FavoritePhoto
import com.yapp2app.photo.domain.entity.PhotoImage
import com.yapp2app.user.domain.entity.User
import io.restassured.RestAssured
import org.hamcrest.Matchers.containsString
import org.hamcrest.Matchers.equalTo
import org.hamcrest.Matchers.nullValue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.http.HttpStatus
import org.springframework.test.context.ActiveProfiles

/**
 * fileName       : GetFavoriteSummaryE2ETest
 * author         : claude
 * date           : 2026. 1. 22.
 * description    : 즐겨찾기 사진 요약 정보 조회 E2E 테스트
 */
@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class GetFavoriteSummaryE2ETest : PhotoImageE2ETestBase() {

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
    @DisplayName("즐겨찾기 사진 요약 조회 성공 - 즐겨찾기 있는 경우")
    fun givenFavoritePhotosExist_whenGetFavoriteSummary_thenReturnsSummary() {
        // given
        val media1 = createMedia(ownerId = testUser.id!!, status = MediaStatus.UPLOADED)
        val media2 = createMedia(ownerId = testUser.id!!, status = MediaStatus.UPLOADED)
        Thread.sleep(10)
        val media3 = createMedia(ownerId = testUser.id!!, status = MediaStatus.UPLOADED)

        createFavoritePhotoImage(userId = testUser.id!!, mediaId = media1.id!!)
        createFavoritePhotoImage(userId = testUser.id!!, mediaId = media2.id!!)
        val latestPhoto = createFavoritePhotoImage(userId = testUser.id!!, mediaId = media3.id!!)

        // when & then
        RestAssured.given()
            .header("Authorization", "Bearer $accessToken")
            .`when`()
            .get("/api/photos/favorite/summary")
            .then()
            .statusCode(HttpStatus.OK.value())
            .body("resultCode", equalTo(ResultCode.SUCCESS.code))
            .body("data.totalCount", equalTo(3))
            .body("data.latestImageUrl", containsString(media3.storageKey))
    }

    @Test
    @DisplayName("즐겨찾기 사진 요약 조회 성공 - 즐겨찾기 없는 경우")
    fun givenNoFavoritePhotos_whenGetFavoriteSummary_thenReturnsEmptySummary() {
        // given: 즐겨찾기 없음

        // when & then
        RestAssured.given()
            .header("Authorization", "Bearer $accessToken")
            .`when`()
            .get("/api/photos/favorite/summary")
            .then()
            .statusCode(HttpStatus.OK.value())
            .body("resultCode", equalTo(ResultCode.SUCCESS.code))
            .body("data.totalCount", equalTo(0))
            .body("data.latestImageUrl", nullValue())
    }

    @Test
    @DisplayName("즐겨찾기 사진 요약 조회 성공 - 일반 사진만 있고 즐겨찾기 없는 경우")
    fun givenOnlyNonFavoritePhotos_whenGetFavoriteSummary_thenReturnsEmptySummary() {
        // given: 일반 사진만 존재
        val media1 = createMedia(ownerId = testUser.id!!, status = MediaStatus.UPLOADED)
        val media2 = createMedia(ownerId = testUser.id!!, status = MediaStatus.UPLOADED)
        createPhotoImage(userId = testUser.id!!, mediaId = media1.id!!)
        createPhotoImage(userId = testUser.id!!, mediaId = media2.id!!)

        // when & then
        RestAssured.given()
            .header("Authorization", "Bearer $accessToken")
            .`when`()
            .get("/api/photos/favorite/summary")
            .then()
            .statusCode(HttpStatus.OK.value())
            .body("resultCode", equalTo(ResultCode.SUCCESS.code))
            .body("data.totalCount", equalTo(0))
            .body("data.latestImageUrl", nullValue())
    }

    @Test
    @DisplayName("즐겨찾기 사진 요약 조회 - 토큰 없이 요청 시 403 에러")
    fun givenNoToken_whenGetFavoriteSummary_thenReturnsForbidden() {
        // when & then
        RestAssured.given()
            .`when`()
            .get("/api/photos/favorite/summary")
            .then()
            .statusCode(HttpStatus.FORBIDDEN.value())
    }

    @Test
    @DisplayName("즐겨찾기 사진 요약 조회 - 가장 최근 이미지 확인")
    fun givenMultipleFavoritePhotos_whenGetFavoriteSummary_thenReturnsLatestImage() {
        // given: 시간 간격을 두고 3개의 즐겨찾기 사진 생성
        val media1 = createMedia(ownerId = testUser.id!!, status = MediaStatus.UPLOADED)
        createFavoritePhotoImage(userId = testUser.id!!, mediaId = media1.id!!)

        Thread.sleep(10)

        val media2 = createMedia(ownerId = testUser.id!!, status = MediaStatus.UPLOADED)
        createFavoritePhotoImage(userId = testUser.id!!, mediaId = media2.id!!)

        Thread.sleep(10)

        val latestMedia = createMedia(ownerId = testUser.id!!, status = MediaStatus.UPLOADED)
        createFavoritePhotoImage(userId = testUser.id!!, mediaId = latestMedia.id!!)

        // when & then: 가장 최근 미디어(latestMedia)의 storageKey가 URL에 포함되어야 함
        RestAssured.given()
            .header("Authorization", "Bearer $accessToken")
            .`when`()
            .get("/api/photos/favorite/summary")
            .then()
            .statusCode(HttpStatus.OK.value())
            .body("resultCode", equalTo(ResultCode.SUCCESS.code))
            .body("data.totalCount", equalTo(3))
            .body("data.latestImageUrl", containsString(latestMedia.storageKey))
    }

    fun createFavoritePhotoImage(userId: Long, mediaId: Long, folderId: Long? = null): PhotoImage {
        val photo = photoImageRepository.save(
            PhotoImage(
                userId = userId,
                mediaId = mediaId,
                folderId = folderId,
            ),
        )
        favoritePhotoRepository.save(FavoritePhoto(userId = userId, imageId = photo.id!!))
        return photo
    }
}
