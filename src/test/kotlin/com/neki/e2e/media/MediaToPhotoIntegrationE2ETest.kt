package com.neki.e2e.media

import com.neki.common.code.ResultCode
import com.neki.media.api.dto.UploadTicketRequest
import com.neki.media.domain.MediaType
import com.neki.media.domain.entity.MediaStatus
import com.neki.photo.api.dto.UploadPhotoRequest
import com.neki.photo.domain.entity.Folder
import com.neki.photo.domain.enums.UploadMethod
import com.neki.user.domain.entity.User
import io.restassured.RestAssured
import io.restassured.http.ContentType
import org.assertj.core.api.Assertions.assertThat
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
 * fileName       : MediaToPhotoIntegrationE2ETest
 * author         : koo
 * date           : 2026. 1. 23.
 * description    : Media 업로드 ticket 발급부터 Photo 등록까지의 통합 E2E 테스트
 */
@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class MediaToPhotoIntegrationE2ETest : MediaE2ETestBase() {

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
    @DisplayName("전체 워크플로우 테스트 - 벌크 ticket 발급 → S3 업로드 시뮬레이션 → 벌크 photo 등록")
    fun givenCompleteWorkflow_whenUploadTicketAndUploadPhoto_thenPhotosCreatedAndMediaStatusUpdated() {
        // Step 1: POST /api/media → mediaIds와 uploadTickets 받기
        val ticketRequest = UploadTicketRequest(
            items = listOf(
                UploadTicketRequest.UploadTicketItem(
                    filename = "photo1.jpg",
                    contentType = "image/jpeg",
                    mediaType = MediaType.PHOTO_BOOTH,
                ),
                UploadTicketRequest.UploadTicketItem(
                    filename = "photo2.png",
                    contentType = "image/png",
                    mediaType = MediaType.PHOTO_BOOTH,
                ),
                UploadTicketRequest.UploadTicketItem(
                    filename = "photo3.jpg",
                    contentType = "image/jpeg",
                    mediaType = MediaType.PHOTO_BOOTH,
                ),
            ),
        )

        val mediaIds = RestAssured.given()
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer $accessToken")
            .body(ticketRequest)
            .`when`()
            .post("/api/media/upload")
            .then()
            .statusCode(HttpStatus.OK.value())
            .body("data.items", hasSize<Any>(3))
            .extract()
            .jsonPath()
            .getList<Int>("data.items.mediaId")
            .map { it.toLong() }

        // Step 2: S3 업로드 시뮬레이션 (LocalMediaClient가 자동으로 UPLOADED로 변경)
        // LocalMediaClient는 S3 실제 업로드 없이도 Media 상태를 UPLOADED로 변경함
        // 이는 테스트 환경에서 S3를 사용하지 않고도 검증할 수 있게 함

        // Step 3: POST /api/photos/bulk 호출하여 메타데이터 등록
        val uploadPhotoRequest = UploadPhotoRequest(
            folderId = null,
            uploads = mediaIds.map {
                UploadPhotoRequest.UploadPhotoItem(
                    mediaId = it,
                    uploadMethod = UploadMethod.DIRECT_UPLOAD,
                    memo = null,
                    capturedAt = null,
                )
            },
        )

        RestAssured.given()
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer $accessToken")
            .body(uploadPhotoRequest)
            .`when`()
            .post("/api/photos")
            .then()
            .statusCode(HttpStatus.OK.value())
            .body("resultCode", equalTo(ResultCode.SUCCESS.code))

        // Step 4: 검증 - Photos가 생성되었고 Media 상태가 UPLOADED로 업데이트되었는지 확인
        val savedPhotos = photoImageRepository.findAll().filter { it.userId == testUser.id }
        assertThat(savedPhotos).hasSize(3)

        mediaIds.forEach { mediaId ->
            val media = mediaRepository.findById(mediaId).orElseThrow()
            assertThat(media.status).isEqualTo(MediaStatus.UPLOADED)
            assertThat(media.ownerId).isEqualTo(testUser.id)
        }
    }

    @Test
    @DisplayName("워크플로우 with Folder - 벌크 ticket 발급 → 특정 폴더에 photo 등록")
    fun givenWorkflowWithFolder_whenUploadTicketAndUploadPhoto_thenPhotosCreatedInFolder() {
        // given - 폴더 생성
        val folder = createFolder(testUser.id!!, "테스트 폴더")

        // Step 1: POST /api/media → mediaIds 받기
        val ticketRequest = UploadTicketRequest(
            items = listOf(
                UploadTicketRequest.UploadTicketItem(
                    filename = "photo1.jpg",
                    contentType = "image/jpeg",
                    mediaType = MediaType.PHOTO_BOOTH,
                ),
                UploadTicketRequest.UploadTicketItem(
                    filename = "photo2.jpg",
                    contentType = "image/jpeg",
                    mediaType = MediaType.PHOTO_BOOTH,
                ),
            ),
        )

        val mediaIds = RestAssured.given()
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer $accessToken")
            .body(ticketRequest)
            .`when`()
            .post("/api/media/upload")
            .then()
            .statusCode(HttpStatus.OK.value())
            .extract()
            .jsonPath()
            .getList<Int>("data.items.mediaId")
            .map { it.toLong() }

        // Step 2: POST /api/photos/bulk 호출하여 folderId와 함께 메타데이터 등록
        val uploadPhotoRequest = UploadPhotoRequest(
            folderId = folder.id,
            uploads = mediaIds.map {
                UploadPhotoRequest.UploadPhotoItem(
                    mediaId = it,
                    uploadMethod = UploadMethod.DIRECT_UPLOAD,
                    memo = "테스트 메모",
                    capturedAt = null,
                )
            },
        )

        RestAssured.given()
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer $accessToken")
            .body(uploadPhotoRequest)
            .`when`()
            .post("/api/photos")
            .then()
            .statusCode(HttpStatus.OK.value())
            .body("resultCode", equalTo(ResultCode.SUCCESS.code))

        // Step 3: 검증 - Photos가 올바른 폴더에 생성되었는지 확인
        val savedPhotos = photoImageRepository.findAll().filter { it.userId == testUser.id }
        assertThat(savedPhotos).hasSize(2)

        val folderPhotoIds: Set<Long> = photoImageFolderRepository.findAllByFolderIdIn(listOf(folder.id!!))
            .map { it.photoImageId }.toSet()

        savedPhotos.forEach { photo ->
            assertThat(folderPhotoIds).contains(photo.id)
            assertThat(photo.memo).isEqualTo("테스트 메모")
            assertThat(photo.userId).isEqualTo(testUser.id)
        }

        // 중간 테이블에서 폴더 연관 확인
        val folderRelations = photoImageFolderRepository.findAllByFolderIdIn(listOf(folder.id!!))
        assertThat(folderRelations).hasSize(2)
    }

    @Test
    @DisplayName("혼합 mediaType 테스트 - 서로 다른 mediaType으로 ticket 발급 후 photo 등록")
    fun givenMixedMediaTypes_whenUploadTicketAndUploadPhoto_thenPhotosCreatedWithDifferentMediaTypes() {
        // Step 1: 서로 다른 mediaType으로 ticket 발급
        val ticketRequest = UploadTicketRequest(
            items = listOf(
                UploadTicketRequest.UploadTicketItem(
                    filename = "photo1.jpg",
                    contentType = "image/jpeg",
                    mediaType = MediaType.PHOTO_BOOTH,
                ),
                UploadTicketRequest.UploadTicketItem(
                    filename = "attachment1.png",
                    contentType = "image/png",
                    mediaType = MediaType.ATTACHMENT,
                ),
            ),
        )

        val mediaIds = RestAssured.given()
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer $accessToken")
            .body(ticketRequest)
            .`when`()
            .post("/api/media/upload")
            .then()
            .statusCode(HttpStatus.OK.value())
            .extract()
            .jsonPath()
            .getList<Int>("data.items.mediaId")
            .map { it.toLong() }

        // Step 2: photo 등록
        val uploadPhotoRequest = UploadPhotoRequest(
            folderId = null,
            uploads = mediaIds.map {
                UploadPhotoRequest.UploadPhotoItem(
                    mediaId = it,
                    uploadMethod = UploadMethod.DIRECT_UPLOAD,
                    memo = null,
                    capturedAt = null,
                )
            },
        )

        RestAssured.given()
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer $accessToken")
            .body(uploadPhotoRequest)
            .`when`()
            .post("/api/photos")
            .then()
            .statusCode(HttpStatus.OK.value())

        // Step 3: 검증 - 각 Media의 mediaType이 올바르게 저장되었는지 확인
        val media1 = mediaRepository.findById(mediaIds[0]).orElseThrow()
        assertThat(media1.mediaType).isEqualTo(MediaType.PHOTO_BOOTH)

        val media2 = mediaRepository.findById(mediaIds[1]).orElseThrow()
        assertThat(media2.mediaType).isEqualTo(MediaType.ATTACHMENT)
    }

    @Test
    @DisplayName("다른 사용자의 폴더에 업로드 시도 시 NOT_FOUND 반환")
    fun givenOtherUsersFolder_whenUploadPhoto_thenNotFound() {
        // given
        val (otherUser, _) = createTestUserAndToken(email = "other-${System.currentTimeMillis()}@example.com")
        val otherUsersFolder = createFolder(otherUser.id!!, "다른 사용자의 폴더")

        // ticket 발급
        val ticketRequest = UploadTicketRequest(
            items = listOf(
                UploadTicketRequest.UploadTicketItem(
                    filename = "photo1.jpg",
                    contentType = "image/jpeg",
                    mediaType = MediaType.PHOTO_BOOTH,
                ),
            ),
        )

        val mediaIds = RestAssured.given()
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer $accessToken")
            .body(ticketRequest)
            .`when`()
            .post("/api/media/upload")
            .then()
            .statusCode(HttpStatus.OK.value())
            .extract()
            .jsonPath()
            .getList<Int>("data.items.mediaId")
            .map { it.toLong() }

        // when
        val uploadPhotoRequest = UploadPhotoRequest(
            folderId = otherUsersFolder.id,
            uploads = mediaIds.map {
                UploadPhotoRequest.UploadPhotoItem(
                    mediaId = it,
                    uploadMethod = UploadMethod.DIRECT_UPLOAD,
                    memo = null,
                    capturedAt = null,
                )
            },
        )

        // then
        RestAssured.given()
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer $accessToken")
            .body(uploadPhotoRequest)
            .`when`()
            .post("/api/photos")
            .then()
            .statusCode(HttpStatus.BAD_REQUEST.value())
            .body("resultCode", equalTo(ResultCode.NOT_FOUND.code))
    }

    @Test
    @DisplayName("존재하지 않는 폴더에 업로드 시도 시 NOT_FOUND 반환")
    fun givenNonExistentFolder_whenUploadPhoto_thenNotFound() {
        // given
        val ticketRequest = UploadTicketRequest(
            items = listOf(
                UploadTicketRequest.UploadTicketItem(
                    filename = "photo1.jpg",
                    contentType = "image/jpeg",
                    mediaType = MediaType.PHOTO_BOOTH,
                ),
            ),
        )

        val mediaIds = RestAssured.given()
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer $accessToken")
            .body(ticketRequest)
            .`when`()
            .post("/api/media/upload")
            .then()
            .statusCode(HttpStatus.OK.value())
            .extract()
            .jsonPath()
            .getList<Int>("data.items.mediaId")
            .map { it.toLong() }

        // when
        val uploadPhotoRequest = UploadPhotoRequest(
            folderId = 999999L,
            uploads = mediaIds.map {
                UploadPhotoRequest.UploadPhotoItem(
                    mediaId = it,
                    uploadMethod = UploadMethod.DIRECT_UPLOAD,
                    memo = null,
                    capturedAt = null,
                )
            },
        )

        // then
        RestAssured.given()
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer $accessToken")
            .body(uploadPhotoRequest)
            .`when`()
            .post("/api/photos")
            .then()
            .statusCode(HttpStatus.BAD_REQUEST.value())
            .body("resultCode", equalTo(ResultCode.NOT_FOUND.code))
    }

    private fun createFolder(userId: Long, name: String): Folder = folderRepository.save(
        Folder(
            userId = userId,
            name = name,
        ),
    )
}
