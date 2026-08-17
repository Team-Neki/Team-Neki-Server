package com.neki.api.e2e.photo.image

import com.neki.core.code.ResultCode
import com.neki.domain.media.models.MediaStatus
import com.neki.domain.photo.models.PhotoImageFolder
import com.neki.domain.user.models.User
import io.restassured.RestAssured
import org.hamcrest.Matchers.empty
import org.hamcrest.Matchers.equalTo
import org.hamcrest.Matchers.hasItem
import org.hamcrest.Matchers.hasSize
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
    @DisplayName("사진 목록 조회 성공 - 전체 사진 조회 및 imageUrl이 /file/image/ 패턴을 따름")
    fun givenPhotosExist_whenGetPhotos_thenReturnsPhotoListWithFileImageUrl() {
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
            .body("data.items[0].imageUrl", startsWith("/file/image/"))
            .body("data.items[1].imageUrl", startsWith("/file/image/"))
    }

    @Test
    @DisplayName("사진 목록 조회 성공 - 여러 사진 조회")
    fun givenMultiplePhotos_whenGetPhotos_thenReturnsAllPhotos() {
        // given
        val media1 = createMedia(ownerId = testUser.id!!, status = MediaStatus.UPLOADED)
        val media2 = createMedia(ownerId = testUser.id!!, status = MediaStatus.UPLOADED)
        val media3 = createMedia(ownerId = testUser.id!!, status = MediaStatus.UPLOADED)

        createPhotoImage(userId = testUser.id!!, mediaId = media1.id!!)
        createPhotoImage(userId = testUser.id!!, mediaId = media2.id!!)
        createPhotoImage(userId = testUser.id!!, mediaId = media3.id!!)

        // when & then
        RestAssured.given()
            .header("Authorization", "Bearer $accessToken")
            .`when`()
            .get("/api/photos")
            .then()
            .statusCode(HttpStatus.OK.value())
            .body("resultCode", equalTo(ResultCode.SUCCESS.code))
            .body("data.items", hasSize<Int>(3))
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

    // =====================
    // 페이징 테스트
    // =====================

    @Test
    @DisplayName("사진 목록 페이징 조회 - 첫 페이지")
    fun givenMultiplePhotos_whenGetFirstPage_thenReturnsPagedResults() {
        // given: 3개의 사진 생성
        repeat(3) {
            val media = createMedia(ownerId = testUser.id!!, status = MediaStatus.UPLOADED)
            createPhotoImage(userId = testUser.id!!, mediaId = media.id!!)
        }

        // when & then: page=0, size=2 조회 시 2개 반환 + hasNext=true
        RestAssured.given()
            .header("Authorization", "Bearer $accessToken")
            .queryParam("page", 0)
            .queryParam("size", 2)
            .`when`()
            .get("/api/photos")
            .then()
            .statusCode(HttpStatus.OK.value())
            .body("resultCode", equalTo(ResultCode.SUCCESS.code))
            .body("data.items", hasSize<Int>(2))
            .body("data.hasNext", equalTo(true))
    }

    @Test
    @DisplayName("사진 목록 페이징 조회 - 마지막 페이지")
    fun givenMultiplePhotos_whenGetLastPage_thenReturnsRemainingResults() {
        // given: 3개의 사진 생성
        repeat(3) {
            val media = createMedia(ownerId = testUser.id!!, status = MediaStatus.UPLOADED)
            createPhotoImage(userId = testUser.id!!, mediaId = media.id!!)
        }

        // when & then: page=1, size=2 조회 시 1개 반환 + hasNext=false
        RestAssured.given()
            .header("Authorization", "Bearer $accessToken")
            .queryParam("page", 1)
            .queryParam("size", 2)
            .`when`()
            .get("/api/photos")
            .then()
            .statusCode(HttpStatus.OK.value())
            .body("resultCode", equalTo(ResultCode.SUCCESS.code))
            .body("data.items", hasSize<Int>(1))
            .body("data.hasNext", equalTo(false))
    }

    @Test
    @DisplayName("사진 목록 페이징 조회 - 빈 페이지")
    fun givenMultiplePhotos_whenGetEmptyPage_thenReturnsEmptyList() {
        // given: 2개의 사진 생성
        repeat(2) {
            val media = createMedia(ownerId = testUser.id!!, status = MediaStatus.UPLOADED)
            createPhotoImage(userId = testUser.id!!, mediaId = media.id!!)
        }

        // when & then: page=5, size=2 조회 시 빈 목록 + hasNext=false
        RestAssured.given()
            .header("Authorization", "Bearer $accessToken")
            .queryParam("page", 5)
            .queryParam("size", 2)
            .`when`()
            .get("/api/photos")
            .then()
            .statusCode(HttpStatus.OK.value())
            .body("resultCode", equalTo(ResultCode.SUCCESS.code))
            .body("data.items", hasSize<Int>(0))
            .body("data.hasNext", equalTo(false))
    }

    @Test
    @DisplayName("사진 목록 페이징 조회 - 기본 파라미터")
    fun givenMultiplePhotos_whenGetWithoutPagination_thenReturnsDefaultPageSize() {
        // given: 25개의 사진 생성 (default size=20보다 많음)
        repeat(25) {
            val media = createMedia(ownerId = testUser.id!!, status = MediaStatus.UPLOADED)
            createPhotoImage(userId = testUser.id!!, mediaId = media.id!!)
        }

        // when & then: 파라미터 없이 조회 시 20개 반환 + hasNext=true
        RestAssured.given()
            .header("Authorization", "Bearer $accessToken")
            .`when`()
            .get("/api/photos")
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
        @DisplayName("사진 목록 정렬 - 최신순(DESC) 기본값")
        fun givenMultiplePhotos_whenGetWithDefaultSort_thenReturnsDescOrder() {
            // given: 순서대로 3개의 사진 생성 (생성 시간 차이 확보)
            val photos = (1..3).map {
                val media = createMedia(ownerId = testUser.id!!, status = MediaStatus.UPLOADED)
                Thread.sleep(10) // 생성 시간 차이 확보
                createPhotoImage(userId = testUser.id!!, mediaId = media.id!!)
            }

            // when & then: 기본 정렬(DESC)로 조회 시 최신 사진이 먼저 나옴
            RestAssured.given()
                .header("Authorization", "Bearer $accessToken")
                .`when`()
                .get("/api/photos")
                .then()
                .statusCode(HttpStatus.OK.value())
                .body("resultCode", equalTo(ResultCode.SUCCESS.code))
                .body("data.items", hasSize<Int>(3))
                .body("data.items[0].photoId", equalTo(photos[2].id!!.toInt()))
                .body("data.items[1].photoId", equalTo(photos[1].id!!.toInt()))
                .body("data.items[2].photoId", equalTo(photos[0].id!!.toInt()))
        }

        @Test
        @DisplayName("사진 목록 정렬 - 최신순(DESC) 명시적 지정")
        fun givenMultiplePhotos_whenGetWithDescSort_thenReturnsDescOrder() {
            // given: 순서대로 3개의 사진 생성
            val photos = (1..3).map {
                val media = createMedia(ownerId = testUser.id!!, status = MediaStatus.UPLOADED)
                Thread.sleep(10)
                createPhotoImage(userId = testUser.id!!, mediaId = media.id!!)
            }

            // when & then: DESC 정렬로 조회 시 최신 사진이 먼저 나옴
            RestAssured.given()
                .header("Authorization", "Bearer $accessToken")
                .queryParam("sortOrder", "DESC")
                .`when`()
                .get("/api/photos")
                .then()
                .statusCode(HttpStatus.OK.value())
                .body("resultCode", equalTo(ResultCode.SUCCESS.code))
                .body("data.items", hasSize<Int>(3))
                .body("data.items[0].photoId", equalTo(photos[2].id!!.toInt()))
                .body("data.items[1].photoId", equalTo(photos[1].id!!.toInt()))
                .body("data.items[2].photoId", equalTo(photos[0].id!!.toInt()))
        }

        @Test
        @DisplayName("사진 목록 정렬 - 오래된순(ASC)")
        fun givenMultiplePhotos_whenGetWithAscSort_thenReturnsAscOrder() {
            // given: 순서대로 3개의 사진 생성
            val photos = (1..3).map {
                val media = createMedia(ownerId = testUser.id!!, status = MediaStatus.UPLOADED)
                Thread.sleep(10)
                createPhotoImage(userId = testUser.id!!, mediaId = media.id!!)
            }

            // when & then: ASC 정렬로 조회 시 오래된 사진이 먼저 나옴
            RestAssured.given()
                .header("Authorization", "Bearer $accessToken")
                .queryParam("sortOrder", "ASC")
                .`when`()
                .get("/api/photos")
                .then()
                .statusCode(HttpStatus.OK.value())
                .body("resultCode", equalTo(ResultCode.SUCCESS.code))
                .body("data.items", hasSize<Int>(3))
                .body("data.items[0].photoId", equalTo(photos[0].id!!.toInt()))
                .body("data.items[1].photoId", equalTo(photos[1].id!!.toInt()))
                .body("data.items[2].photoId", equalTo(photos[2].id!!.toInt()))
        }

        @Test
        @DisplayName("사진 목록 정렬과 페이징 조합 - DESC + 첫 페이지")
        fun givenMultiplePhotos_whenGetFirstPageWithDescSort_thenReturnsPagedDescOrder() {
            // given: 5개의 사진 생성
            val photos = (1..5).map {
                val media = createMedia(ownerId = testUser.id!!, status = MediaStatus.UPLOADED)
                Thread.sleep(10)
                createPhotoImage(userId = testUser.id!!, mediaId = media.id!!)
            }

            // when & then: page=0, size=2, DESC 조회 시 최신 2개 반환
            RestAssured.given()
                .header("Authorization", "Bearer $accessToken")
                .queryParam("page", 0)
                .queryParam("size", 2)
                .queryParam("sortOrder", "DESC")
                .`when`()
                .get("/api/photos")
                .then()
                .statusCode(HttpStatus.OK.value())
                .body("resultCode", equalTo(ResultCode.SUCCESS.code))
                .body("data.items", hasSize<Int>(2))
                .body("data.hasNext", equalTo(true))
                .body("data.items[0].photoId", equalTo(photos[4].id!!.toInt()))
                .body("data.items[1].photoId", equalTo(photos[3].id!!.toInt()))
        }

        @Test
        @DisplayName("사진 목록 정렬과 페이징 조합 - ASC + 첫 페이지")
        fun givenMultiplePhotos_whenGetFirstPageWithAscSort_thenReturnsPagedAscOrder() {
            // given: 5개의 사진 생성
            val photos = (1..5).map {
                val media = createMedia(ownerId = testUser.id!!, status = MediaStatus.UPLOADED)
                Thread.sleep(10)
                createPhotoImage(userId = testUser.id!!, mediaId = media.id!!)
            }

            // when & then: page=0, size=2, ASC 조회 시 오래된 2개 반환
            RestAssured.given()
                .header("Authorization", "Bearer $accessToken")
                .queryParam("page", 0)
                .queryParam("size", 2)
                .queryParam("sortOrder", "ASC")
                .`when`()
                .get("/api/photos")
                .then()
                .statusCode(HttpStatus.OK.value())
                .body("resultCode", equalTo(ResultCode.SUCCESS.code))
                .body("data.items", hasSize<Int>(2))
                .body("data.hasNext", equalTo(true))
                .body("data.items[0].photoId", equalTo(photos[0].id!!.toInt()))
                .body("data.items[1].photoId", equalTo(photos[1].id!!.toInt()))
        }

        @Test
        @DisplayName("사진 목록 조회와 ASC 정렬 조합")
        fun givenMultiplePhotos_whenGetWithAscSort_thenReturnsSortedPhotos() {
            // given
            val photos = (1..3).map {
                val media = createMedia(ownerId = testUser.id!!, status = MediaStatus.UPLOADED)
                Thread.sleep(10)
                createPhotoImage(userId = testUser.id!!, mediaId = media.id!!)
            }

            // when & then: ASC 정렬로 조회
            RestAssured.given()
                .header("Authorization", "Bearer $accessToken")
                .queryParam("sortOrder", "ASC")
                .`when`()
                .get("/api/photos")
                .then()
                .statusCode(HttpStatus.OK.value())
                .body("resultCode", equalTo(ResultCode.SUCCESS.code))
                .body("data.items", hasSize<Int>(3))
                .body("data.items[0].photoId", equalTo(photos[0].id!!.toInt()))
                .body("data.items[1].photoId", equalTo(photos[1].id!!.toInt()))
                .body("data.items[2].photoId", equalTo(photos[2].id!!.toInt()))
        }
    }

    // =====================
    // 폴더 필터 테스트
    // =====================

    @Nested
    @DisplayName("폴더 필터 테스트")
    inner class FolderFilterTests {

        @Test
        @DisplayName("folderId 필터로 조회 시 해당 폴더의 사진만 반환된다")
        fun givenPhotosInFolder_whenListWithFolderId_thenOnlyFolderPhotosReturned() {
            // given: 폴더에 사진 2장, 폴더 밖에 사진 1장
            val folder = createFolder(testUser.id!!, "테스트 폴더")
            val media1 = createMedia(ownerId = testUser.id!!, status = MediaStatus.UPLOADED)
            val media2 = createMedia(ownerId = testUser.id!!, status = MediaStatus.UPLOADED)
            val media3 = createMedia(ownerId = testUser.id!!, status = MediaStatus.UPLOADED)

            val folderPhoto1 = createPhotoImage(userId = testUser.id!!, mediaId = media1.id!!, folderId = folder.id)
            val folderPhoto2 = createPhotoImage(userId = testUser.id!!, mediaId = media2.id!!, folderId = folder.id)
            createPhotoImage(userId = testUser.id!!, mediaId = media3.id!!) // 폴더 밖 사진

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
                .body("data.items.photoId", hasItem(folderPhoto1.id!!.toInt()))
                .body("data.items.photoId", hasItem(folderPhoto2.id!!.toInt()))
        }

        @Test
        @DisplayName("빈 폴더를 folderId로 조회 시 빈 결과를 반환한다")
        fun givenEmptyFolder_whenListWithFolderId_thenEmptyResult() {
            // given: 빈 폴더
            val folder = createFolder(testUser.id!!, "빈 폴더")

            // when & then
            RestAssured.given()
                .header("Authorization", "Bearer $accessToken")
                .queryParam("folderId", folder.id)
                .`when`()
                .get("/api/photos")
                .then()
                .statusCode(HttpStatus.OK.value())
                .body("resultCode", equalTo(ResultCode.SUCCESS.code))
                .body("data.items", empty<Any>())
        }

        @Test
        @DisplayName("folderId 필터와 페이징이 함께 동작한다")
        fun givenPhotosInFolder_whenListWithFolderIdAndPagination_thenPaginatedCorrectly() {
            // given: 폴더에 사진 3장
            val folder = createFolder(testUser.id!!, "페이징 폴더")
            repeat(3) {
                val media = createMedia(ownerId = testUser.id!!, status = MediaStatus.UPLOADED)
                createPhotoImage(userId = testUser.id!!, mediaId = media.id!!, folderId = folder.id)
            }

            // when & then: page=0, size=2
            RestAssured.given()
                .header("Authorization", "Bearer $accessToken")
                .queryParam("folderId", folder.id)
                .queryParam("page", 0)
                .queryParam("size", 2)
                .`when`()
                .get("/api/photos")
                .then()
                .statusCode(HttpStatus.OK.value())
                .body("resultCode", equalTo(ResultCode.SUCCESS.code))
                .body("data.items", hasSize<Int>(2))
                .body("data.hasNext", equalTo(true))

            // page=1, size=2: 나머지 1장
            RestAssured.given()
                .header("Authorization", "Bearer $accessToken")
                .queryParam("folderId", folder.id)
                .queryParam("page", 1)
                .queryParam("size", 2)
                .`when`()
                .get("/api/photos")
                .then()
                .statusCode(HttpStatus.OK.value())
                .body("resultCode", equalTo(ResultCode.SUCCESS.code))
                .body("data.items", hasSize<Int>(1))
                .body("data.hasNext", equalTo(false))
        }

        @Test
        @DisplayName("같은 사진이 여러 폴더에 속한 경우 각 폴더 조회 시 해당 사진이 반환된다")
        fun givenPhotoInMultipleFolders_whenListByFolder_thenPhotoAppearsInEach() {
            // given: 사진 1장을 폴더 A, B에 모두 연결
            val folderA = createFolder(testUser.id!!, "폴더 A")
            val folderB = createFolder(testUser.id!!, "폴더 B")
            val media = createMedia(ownerId = testUser.id!!, status = MediaStatus.UPLOADED)
            val photo = createPhotoImage(userId = testUser.id!!, mediaId = media.id!!, folderId = folderA.id)
            // 폴더 B에도 연결
            photoImageFolderRepository.save(PhotoImageFolder(photoImageId = photo.id!!, folderId = folderB.id!!))

            // when & then: 폴더 A 조회
            RestAssured.given()
                .header("Authorization", "Bearer $accessToken")
                .queryParam("folderId", folderA.id)
                .`when`()
                .get("/api/photos")
                .then()
                .statusCode(HttpStatus.OK.value())
                .body("data.items", hasSize<Int>(1))
                .body("data.items[0].photoId", equalTo(photo.id!!.toInt()))

            // when & then: 폴더 B 조회
            RestAssured.given()
                .header("Authorization", "Bearer $accessToken")
                .queryParam("folderId", folderB.id)
                .`when`()
                .get("/api/photos")
                .then()
                .statusCode(HttpStatus.OK.value())
                .body("data.items", hasSize<Int>(1))
                .body("data.items[0].photoId", equalTo(photo.id!!.toInt()))
        }
    }
}
