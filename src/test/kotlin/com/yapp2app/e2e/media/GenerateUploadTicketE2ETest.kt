package com.yapp2app.e2e.media

import com.yapp2app.common.api.dto.ResultCode
import com.yapp2app.media.api.dto.GenerateUploadTicketRequest
import com.yapp2app.media.domain.MediaType
import com.yapp2app.media.domain.entity.MediaStatus
import com.yapp2app.user.domain.entity.User
import io.restassured.RestAssured
import io.restassured.http.ContentType
import org.assertj.core.api.Assertions.assertThat
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
 * fileName       : GenerateUploadTicketE2ETest
 * author         : koo
 * date           : 2026. 1. 20.
 * description    : POST /api/media/upload E2E 테스트
 */
@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class GenerateUploadTicketE2ETest : MediaE2ETestBase() {

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
    @DisplayName("presigned URL 발급 성공 - Media가 INITIATED 상태로 생성된다")
    fun givenValidRequest_whenGenerateUploadTicket_thenMediaCreatedWithInitiatedStatus() {
        // given
        val request = GenerateUploadTicketRequest(
            filename = "test-photo.jpg",
            contentType = "image/jpeg",
            mediaType = MediaType.PHOTO_BOOTH,
        )

        // when
        val mediaId = RestAssured.given()
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer $accessToken")
            .body(request)
            .`when`()
            .post("/api/media/upload")
            .then()
            .statusCode(HttpStatus.OK.value())
            .body("resultCode", equalTo(ResultCode.SUCCESS.code))
            .body("data.mediaId", notNullValue())
            .body("data.uploadUrl", notNullValue())
            .body("data.method", equalTo("PUT"))
            .extract()
            .path<Int>("data.mediaId")
            .toLong()

        // then - Media가 INITIATED 상태로 생성되었는지 확인
        val media = mediaRepository.findById(mediaId).orElseThrow()
        assertThat(media.status).isEqualTo(MediaStatus.INITIATED)
        assertThat(media.ownerId).isEqualTo(testUser.id)
        assertThat(media.mediaType).isEqualTo(MediaType.PHOTO_BOOTH)
        assertThat(media.contentType).isEqualTo("image/jpeg")
    }

    @Test
    @DisplayName("filename에 확장자가 있으면 storageKey에 해당 확장자가 포함된다")
    fun givenFilenameWithExtension_whenGenerateUploadTicket_thenStorageKeyContainsExtension() {
        // given
        val request = GenerateUploadTicketRequest(
            filename = "my-photo.png",
            contentType = "image/png",
            mediaType = MediaType.PHOTO_BOOTH,
        )

        // when
        val mediaId = RestAssured.given()
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer $accessToken")
            .body(request)
            .`when`()
            .post("/api/media/upload")
            .then()
            .statusCode(HttpStatus.OK.value())
            .body("resultCode", equalTo(ResultCode.SUCCESS.code))
            .extract()
            .path<Int>("data.mediaId")
            .toLong()

        // then - storageKey에 확장자가 포함되어 있는지 확인
        val media = mediaRepository.findById(mediaId).orElseThrow()
        assertThat(media.storageKey).endsWith(".png")
        assertThat(media.storageKey).startsWith("photo-booth/")
    }

    @Test
    @DisplayName("filename에 확장자가 없으면 contentType에서 확장자를 추출한다")
    fun givenFilenameWithoutExtension_whenGenerateUploadTicket_thenExtractExtensionFromContentType() {
        // given
        val request = GenerateUploadTicketRequest(
            filename = "photo-without-extension",
            contentType = "image/png",
            mediaType = MediaType.PHOTO_BOOTH,
        )

        // when
        val mediaId = RestAssured.given()
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer $accessToken")
            .body(request)
            .`when`()
            .post("/api/media/upload")
            .then()
            .statusCode(HttpStatus.OK.value())
            .body("resultCode", equalTo(ResultCode.SUCCESS.code))
            .extract()
            .path<Int>("data.mediaId")
            .toLong()

        // then - contentType에서 확장자가 추출되어 storageKey에 포함되어 있는지 확인
        val media = mediaRepository.findById(mediaId).orElseThrow()
        assertThat(media.storageKey).endsWith(".png")
        assertThat(media.storageKey).startsWith("photo-booth/")
    }

    @Test
    @DisplayName("image/jpeg contentType은 jpg 확장자로 변환된다")
    fun givenJpegContentType_whenGenerateUploadTicket_thenStorageKeyEndsWithJpg() {
        // given
        val request = GenerateUploadTicketRequest(
            filename = "photo",
            contentType = "image/jpeg",
            mediaType = MediaType.PHOTO_BOOTH,
        )

        // when
        val mediaId = RestAssured.given()
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer $accessToken")
            .body(request)
            .`when`()
            .post("/api/media/upload")
            .then()
            .statusCode(HttpStatus.OK.value())
            .extract()
            .path<Int>("data.mediaId")
            .toLong()

        // then
        val media = mediaRepository.findById(mediaId).orElseThrow()
        assertThat(media.storageKey).endsWith(".jpg")
    }

    @Test
    @DisplayName("인증되지 않은 사용자는 403 에러를 반환한다")
    fun givenNoAuth_whenGenerateUploadTicket_thenReturnsForbidden() {
        // given
        val request = GenerateUploadTicketRequest(
            filename = "test.jpg",
            contentType = "image/jpeg",
            mediaType = MediaType.PHOTO_BOOTH,
        )

        // when & then
        RestAssured.given()
            .contentType(ContentType.JSON)
            .body(request)
            .`when`()
            .post("/api/media/upload")
            .then()
            .statusCode(HttpStatus.FORBIDDEN.value())
    }
}
