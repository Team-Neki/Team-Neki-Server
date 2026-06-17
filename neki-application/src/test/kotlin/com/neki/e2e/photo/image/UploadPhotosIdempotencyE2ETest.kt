package com.neki.e2e.photo.image

import com.neki.common.api.dto.ResultCode
import com.neki.media.MediaType
import com.neki.media.api.dto.UploadTicketRequest
import com.neki.photo.api.dto.UploadPhotoRequest
import com.neki.photo.enums.UploadMethod
import com.neki.photo.infra.persist.jpa.PhotoImageQueryRepository
import com.neki.user.entity.User
import io.restassured.RestAssured
import io.restassured.http.ContentType
import org.assertj.core.api.Assertions.assertThat
import org.hamcrest.Matchers.equalTo
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.http.HttpStatus
import org.springframework.test.context.ActiveProfiles

/**
 * fileName       : UploadPhotosIdempotencyE2ETest
 * author         : claude
 * date           : 2026. 2. 14.
 * description    : POST /api/photos 멱등성 보장 E2E 테스트
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class UploadPhotosIdempotencyE2ETest : PhotoImageE2ETestBase() {

    @LocalServerPort
    private var port: Int = 0

    @Autowired
    private lateinit var photoImageQueryRepository: PhotoImageQueryRepository

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

    // ===================
    // Helper Methods
    // ===================

    private fun createMediaIds(count: Int): List<Long> {
        val ticketRequest = UploadTicketRequest(
            items = (1..count).map {
                UploadTicketRequest.UploadTicketItem(
                    filename = "photo$it.jpg",
                    contentType = "image/jpeg",
                    mediaType = MediaType.PHOTO_BOOTH,
                )
            },
        )

        return RestAssured.given()
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
    }

    private fun uploadPhotos(folderId: Long?, mediaIds: List<Long>) {
        val uploadRequest = UploadPhotoRequest(
            folderId = folderId,
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
            .body(uploadRequest)
            .`when`()
            .post("/api/photos")
            .then()
            .statusCode(HttpStatus.OK.value())
            .body("resultCode", equalTo(ResultCode.SUCCESS.code))
    }

    // ===================
    // Test Cases
    // ===================

    @Test
    @DisplayName("동일 mediaIds로 두 번 호출해도 두 번째 호출이 성공하고 레코드가 중복 생성되지 않는다")
    fun givenAlreadyUploadedPhotos_whenRetryWithSameMediaIds_thenIdempotentSuccess() {
        // given
        val mediaIds = createMediaIds(3)

        // 첫 번째 호출 → 정상 저장
        uploadPhotos(null, mediaIds)
        val countAfterFirst = photoImageQueryRepository.getRegisteredMediaIds(mediaIds).size

        // when: 동일 mediaIds로 두 번째 호출
        uploadPhotos(null, mediaIds)
        val countAfterSecond = photoImageQueryRepository.getRegisteredMediaIds(mediaIds).size

        // then: 레코드 수가 변하지 않아야 함
        assertThat(countAfterFirst).isEqualTo(3)
        assertThat(countAfterSecond).isEqualTo(3)
    }

    @Test
    @DisplayName("폴더 지정 후 동일 mediaIds로 재시도해도 멱등 성공")
    fun givenPhotosInFolder_whenRetryWithSameMediaIds_thenIdempotentSuccess() {
        // given
        val folder = createFolder(testUser.id!!, "테스트 폴더")
        val mediaIds = createMediaIds(2)

        // 첫 번째 호출
        uploadPhotos(folder.id, mediaIds)

        // when: 동일 요청 재시도
        uploadPhotos(folder.id, mediaIds)

        // then
        assertThat(photoImageQueryRepository.getRegisteredMediaIds(mediaIds)).hasSize(2)

        RestAssured.given()
            .header("Authorization", "Bearer $accessToken")
            .queryParam("folderId", folder.id)
            .`when`()
            .get("/api/photos")
            .then()
            .statusCode(HttpStatus.OK.value())
            .body("data.items.size()", equalTo(2))
    }

    @Test
    @DisplayName("부분적으로 저장된 상태에서 전체 mediaIds로 재시도하면 나머지만 저장된다")
    fun givenPartialUpload_whenRetryWithAllMediaIds_thenOnlyNewOnesAreSaved() {
        // given: mediaId 3개 발급 후 앞 2개만 먼저 업로드
        val mediaIds = createMediaIds(3)
        uploadPhotos(null, mediaIds.take(2))

        val countAfterPartial = photoImageQueryRepository.getRegisteredMediaIds(mediaIds).size
        assertThat(countAfterPartial).isEqualTo(2)

        // when: 3개 전부로 재시도
        uploadPhotos(null, mediaIds)

        // then: 3개 레코드가 존재해야 함
        assertThat(photoImageQueryRepository.getRegisteredMediaIds(mediaIds)).hasSize(3)
    }

    @Test
    @DisplayName("단건 업로드 후 동일 mediaId로 재시도해도 멱등 성공")
    fun givenSinglePhoto_whenRetryUpload_thenIdempotentSuccess() {
        // given
        val mediaIds = createMediaIds(1)
        uploadPhotos(null, mediaIds)

        // when
        uploadPhotos(null, mediaIds)

        // then
        assertThat(photoImageQueryRepository.getRegisteredMediaIds(mediaIds)).hasSize(1)
    }

    @Test
    @DisplayName("최초 업로드는 정상적으로 저장된다")
    fun givenNewMediaIds_whenUpload_thenAllSaved() {
        // given
        val mediaIds = createMediaIds(3)

        // when
        uploadPhotos(null, mediaIds)

        // then
        val registeredMediaIds = photoImageQueryRepository.getRegisteredMediaIds(mediaIds)
        assertThat(registeredMediaIds).hasSize(3)
        assertThat(registeredMediaIds).isEqualTo(mediaIds.toSet())
    }
}
