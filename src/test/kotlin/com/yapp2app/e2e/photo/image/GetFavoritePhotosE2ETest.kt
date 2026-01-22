package com.yapp2app.e2e.photo.image

import com.yapp2app.common.api.dto.ResultCode
import com.yapp2app.media.domain.entity.MediaStatus
import com.yapp2app.photo.domain.entity.FavoritePhoto
import com.yapp2app.photo.domain.entity.PhotoImage
import com.yapp2app.user.domain.entity.User
import io.restassured.RestAssured
import org.hamcrest.Matchers.equalTo
import org.hamcrest.Matchers.hasSize
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.http.HttpStatus
import org.springframework.test.context.ActiveProfiles

/**
 * fileName       : GetFavoritePhotosE2ETest
 * author         : koo
 * date           : 2026. 1. 14. 오전 1:45
 * description    :
 */
@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class GetFavoritePhotosE2ETest : PhotoImageE2ETestBase() {

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
    @DisplayName("즐겨찾기 사진 목록 조회 성공")
    fun givenPhotosExist_whenGetFavoritePhotos_thenReturnsFavoritePhotoList() {
        // given
        val media1 = createMedia(ownerId = testUser.id!!, status = MediaStatus.UPLOADED)
        val media2 = createMedia(ownerId = testUser.id!!, status = MediaStatus.UPLOADED)
        createFavoritePhotoImage(userId = testUser.id!!, mediaId = media1.id!!)
        createFavoritePhotoImage(userId = testUser.id!!, mediaId = media2.id!!)

        // when & then
        RestAssured.given()
            .header("Authorization", "Bearer $accessToken")
            .`when`()
            .get("/api/photos/favorite")
            .then()
            .statusCode(HttpStatus.OK.value())
            .body("resultCode", equalTo(ResultCode.SUCCESS.code))
            .body("data.items", hasSize<Int>(2))
            .body("data.hasNext", equalTo(false))
    }

    @Test
    @DisplayName("즐겨찾기한 사진만 조회")
    fun givenPhotos_whenGetFavoritePhotos_thenReturnsOnlyFavoritePhotoList() {
        // given
        val media1 = createMedia(ownerId = testUser.id!!, status = MediaStatus.UPLOADED)
        val media2 = createMedia(ownerId = testUser.id!!, status = MediaStatus.UPLOADED)
        createPhotoImage(userId = testUser.id!!, mediaId = media1.id!!)
        createFavoritePhotoImage(userId = testUser.id!!, mediaId = media2.id!!)

        // when & then
        RestAssured.given()
            .header("Authorization", "Bearer $accessToken")
            .`when`()
            .get("/api/photos/favorite")
            .then()
            .statusCode(HttpStatus.OK.value())
            .body("resultCode", equalTo(ResultCode.SUCCESS.code))
            .body("data.items", hasSize<Int>(1))
            .body("data.hasNext", equalTo(false))
    }

    @Test
    @DisplayName("즐겨찾기 사진 목록 페이징 조회 - 첫 페이지")
    fun givenMultipleFavoritePhotos_whenGetFirstPage_thenReturnsPagedResults() {
        // given: 3개의 즐겨찾기 사진 생성
        val media1 = createMedia(ownerId = testUser.id!!, status = MediaStatus.UPLOADED)
        val media2 = createMedia(ownerId = testUser.id!!, status = MediaStatus.UPLOADED)
        val media3 = createMedia(ownerId = testUser.id!!, status = MediaStatus.UPLOADED)
        createFavoritePhotoImage(userId = testUser.id!!, mediaId = media1.id!!)
        createFavoritePhotoImage(userId = testUser.id!!, mediaId = media2.id!!)
        createFavoritePhotoImage(userId = testUser.id!!, mediaId = media3.id!!)

        // when & then: page=0, size=2 조회 시 2개 반환 + hasNext=true
        RestAssured.given()
            .header("Authorization", "Bearer $accessToken")
            .queryParam("page", 0)
            .queryParam("size", 2)
            .`when`()
            .get("/api/photos/favorite")
            .then()
            .statusCode(HttpStatus.OK.value())
            .body("resultCode", equalTo(ResultCode.SUCCESS.code))
            .body("data.items", hasSize<Int>(2))
            .body("data.hasNext", equalTo(true))
    }

    @Test
    @DisplayName("즐겨찾기 사진 목록 페이징 조회 - 마지막 페이지")
    fun givenMultipleFavoritePhotos_whenGetLastPage_thenReturnsRemainingResults() {
        // given: 3개의 즐겨찾기 사진 생성
        val media1 = createMedia(ownerId = testUser.id!!, status = MediaStatus.UPLOADED)
        val media2 = createMedia(ownerId = testUser.id!!, status = MediaStatus.UPLOADED)
        val media3 = createMedia(ownerId = testUser.id!!, status = MediaStatus.UPLOADED)
        createFavoritePhotoImage(userId = testUser.id!!, mediaId = media1.id!!)
        createFavoritePhotoImage(userId = testUser.id!!, mediaId = media2.id!!)
        createFavoritePhotoImage(userId = testUser.id!!, mediaId = media3.id!!)

        // when & then: page=1, size=2 조회 시 1개 반환 + hasNext=false
        RestAssured.given()
            .header("Authorization", "Bearer $accessToken")
            .queryParam("page", 1)
            .queryParam("size", 2)
            .`when`()
            .get("/api/photos/favorite")
            .then()
            .statusCode(HttpStatus.OK.value())
            .body("resultCode", equalTo(ResultCode.SUCCESS.code))
            .body("data.items", hasSize<Int>(1))
            .body("data.hasNext", equalTo(false))
    }

    @Test
    @DisplayName("즐겨찾기 사진 목록 페이징 조회 - 빈 페이지")
    fun givenMultipleFavoritePhotos_whenGetEmptyPage_thenReturnsEmptyList() {
        // given: 2개의 즐겨찾기 사진 생성
        val media1 = createMedia(ownerId = testUser.id!!, status = MediaStatus.UPLOADED)
        val media2 = createMedia(ownerId = testUser.id!!, status = MediaStatus.UPLOADED)
        createFavoritePhotoImage(userId = testUser.id!!, mediaId = media1.id!!)
        createFavoritePhotoImage(userId = testUser.id!!, mediaId = media2.id!!)

        // when & then: page=5, size=2 조회 시 빈 목록 + hasNext=false
        RestAssured.given()
            .header("Authorization", "Bearer $accessToken")
            .queryParam("page", 5)
            .queryParam("size", 2)
            .`when`()
            .get("/api/photos/favorite")
            .then()
            .statusCode(HttpStatus.OK.value())
            .body("resultCode", equalTo(ResultCode.SUCCESS.code))
            .body("data.items", hasSize<Int>(0))
            .body("data.hasNext", equalTo(false))
    }

    @Test
    @DisplayName("즐겨찾기 사진 목록 페이징 조회 - 기본 파라미터")
    fun givenMultipleFavoritePhotos_whenGetWithoutPagination_thenReturnsDefaultPageSize() {
        // given: 25개의 즐겨찾기 사진 생성 (default size=20보다 많음)
        repeat(25) {
            val media = createMedia(ownerId = testUser.id!!, status = MediaStatus.UPLOADED)
            createFavoritePhotoImage(userId = testUser.id!!, mediaId = media.id!!)
        }

        // when & then: 파라미터 없이 조회 시 20개 반환 + hasNext=true
        RestAssured.given()
            .header("Authorization", "Bearer $accessToken")
            .`when`()
            .get("/api/photos/favorite")
            .then()
            .statusCode(HttpStatus.OK.value())
            .body("resultCode", equalTo(ResultCode.SUCCESS.code))
            .body("data.items", hasSize<Int>(20))
            .body("data.hasNext", equalTo(true))
    }

    @Nested
    @DisplayName("정렬 테스트")
    inner class SortingTests {

        @Test
        @DisplayName("즐겨찾기 사진 목록 정렬 - 최신순(DESC) 기본값")
        fun givenMultipleFavoritePhotos_whenGetWithDefaultSort_thenReturnsDescOrder() {
            // given: 순서대로 3개의 즐겨찾기 사진 생성
            val photos = (1..3).map {
                val media = createMedia(ownerId = testUser.id!!, status = MediaStatus.UPLOADED)
                Thread.sleep(10) // 생성 시간 차이 확보
                createFavoritePhotoImage(userId = testUser.id!!, mediaId = media.id!!)
            }

            // when & then: 기본 정렬(DESC)로 조회 시 최신 사진이 먼저 나옴
            RestAssured.given()
                .header("Authorization", "Bearer $accessToken")
                .`when`()
                .get("/api/photos/favorite")
                .then()
                .statusCode(HttpStatus.OK.value())
                .body("resultCode", equalTo(ResultCode.SUCCESS.code))
                .body("data.items", hasSize<Int>(3))
                .body("data.items[0].photoId", equalTo(photos[2].id!!.toInt()))
                .body("data.items[1].photoId", equalTo(photos[1].id!!.toInt()))
                .body("data.items[2].photoId", equalTo(photos[0].id!!.toInt()))
        }

        @Test
        @DisplayName("즐겨찾기 사진 목록 정렬 - 최신순(DESC) 명시적 지정")
        fun givenMultipleFavoritePhotos_whenGetWithDescSort_thenReturnsDescOrder() {
            // given: 순서대로 3개의 즐겨찾기 사진 생성
            val photos = (1..3).map {
                val media = createMedia(ownerId = testUser.id!!, status = MediaStatus.UPLOADED)
                Thread.sleep(10)
                createFavoritePhotoImage(userId = testUser.id!!, mediaId = media.id!!)
            }

            // when & then: DESC 정렬로 조회 시 최신 사진이 먼저 나옴
            RestAssured.given()
                .header("Authorization", "Bearer $accessToken")
                .queryParam("sortOrder", "DESC")
                .`when`()
                .get("/api/photos/favorite")
                .then()
                .statusCode(HttpStatus.OK.value())
                .body("resultCode", equalTo(ResultCode.SUCCESS.code))
                .body("data.items", hasSize<Int>(3))
                .body("data.items[0].photoId", equalTo(photos[2].id!!.toInt()))
                .body("data.items[1].photoId", equalTo(photos[1].id!!.toInt()))
                .body("data.items[2].photoId", equalTo(photos[0].id!!.toInt()))
        }

        @Test
        @DisplayName("즐겨찾기 사진 목록 정렬 - 오래된순(ASC)")
        fun givenMultipleFavoritePhotos_whenGetWithAscSort_thenReturnsAscOrder() {
            // given: 순서대로 3개의 즐겨찾기 사진 생성
            val photos = (1..3).map {
                val media = createMedia(ownerId = testUser.id!!, status = MediaStatus.UPLOADED)
                Thread.sleep(10)
                createFavoritePhotoImage(userId = testUser.id!!, mediaId = media.id!!)
            }

            // when & then: ASC 정렬로 조회 시 오래된 사진이 먼저 나옴
            RestAssured.given()
                .header("Authorization", "Bearer $accessToken")
                .queryParam("sortOrder", "ASC")
                .`when`()
                .get("/api/photos/favorite")
                .then()
                .statusCode(HttpStatus.OK.value())
                .body("resultCode", equalTo(ResultCode.SUCCESS.code))
                .body("data.items", hasSize<Int>(3))
                .body("data.items[0].photoId", equalTo(photos[0].id!!.toInt()))
                .body("data.items[1].photoId", equalTo(photos[1].id!!.toInt()))
                .body("data.items[2].photoId", equalTo(photos[2].id!!.toInt()))
        }

        @Test
        @DisplayName("즐겨찾기 사진 목록 정렬과 페이징 조합 - DESC + 첫 페이지")
        fun givenMultipleFavoritePhotos_whenGetFirstPageWithDescSort_thenReturnsPagedDescOrder() {
            // given: 5개의 즐겨찾기 사진 생성
            val photos = (1..5).map {
                val media = createMedia(ownerId = testUser.id!!, status = MediaStatus.UPLOADED)
                Thread.sleep(10)
                createFavoritePhotoImage(userId = testUser.id!!, mediaId = media.id!!)
            }

            // when & then: page=0, size=2, DESC 조회 시 최신 2개 반환
            RestAssured.given()
                .header("Authorization", "Bearer $accessToken")
                .queryParam("page", 0)
                .queryParam("size", 2)
                .queryParam("sortOrder", "DESC")
                .`when`()
                .get("/api/photos/favorite")
                .then()
                .statusCode(HttpStatus.OK.value())
                .body("resultCode", equalTo(ResultCode.SUCCESS.code))
                .body("data.items", hasSize<Int>(2))
                .body("data.hasNext", equalTo(true))
                .body("data.items[0].photoId", equalTo(photos[4].id!!.toInt()))
                .body("data.items[1].photoId", equalTo(photos[3].id!!.toInt()))
        }

        @Test
        @DisplayName("즐겨찾기 사진 목록 정렬과 페이징 조합 - ASC + 첫 페이지")
        fun givenMultipleFavoritePhotos_whenGetFirstPageWithAscSort_thenReturnsPagedAscOrder() {
            // given: 5개의 즐겨찾기 사진 생성
            val photos = (1..5).map {
                val media = createMedia(ownerId = testUser.id!!, status = MediaStatus.UPLOADED)
                Thread.sleep(10)
                createFavoritePhotoImage(userId = testUser.id!!, mediaId = media.id!!)
            }

            // when & then: page=0, size=2, ASC 조회 시 오래된 2개 반환
            RestAssured.given()
                .header("Authorization", "Bearer $accessToken")
                .queryParam("page", 0)
                .queryParam("size", 2)
                .queryParam("sortOrder", "ASC")
                .`when`()
                .get("/api/photos/favorite")
                .then()
                .statusCode(HttpStatus.OK.value())
                .body("resultCode", equalTo(ResultCode.SUCCESS.code))
                .body("data.items", hasSize<Int>(2))
                .body("data.hasNext", equalTo(true))
                .body("data.items[0].photoId", equalTo(photos[0].id!!.toInt()))
                .body("data.items[1].photoId", equalTo(photos[1].id!!.toInt()))
        }

        @Test
        @DisplayName("즐겨찾기 사진 목록 정렬과 페이징 조합 - ASC + 두 번째 페이지")
        fun givenMultipleFavoritePhotos_whenGetSecondPageWithAscSort_thenReturnsPagedAscOrder() {
            // given: 5개의 즐겨찾기 사진 생성
            val photos = (1..5).map {
                val media = createMedia(ownerId = testUser.id!!, status = MediaStatus.UPLOADED)
                Thread.sleep(10)
                createFavoritePhotoImage(userId = testUser.id!!, mediaId = media.id!!)
            }

            // when & then: page=1, size=2, ASC 조회 시 3, 4번째 사진 반환
            RestAssured.given()
                .header("Authorization", "Bearer $accessToken")
                .queryParam("page", 1)
                .queryParam("size", 2)
                .queryParam("sortOrder", "ASC")
                .`when`()
                .get("/api/photos/favorite")
                .then()
                .statusCode(HttpStatus.OK.value())
                .body("resultCode", equalTo(ResultCode.SUCCESS.code))
                .body("data.items", hasSize<Int>(2))
                .body("data.hasNext", equalTo(true))
                .body("data.items[0].photoId", equalTo(photos[2].id!!.toInt()))
                .body("data.items[1].photoId", equalTo(photos[3].id!!.toInt()))
        }
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
