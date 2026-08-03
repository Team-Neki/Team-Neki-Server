package com.neki.e2e.photo.image

import com.neki.common.code.ResultCode
import com.neki.media.models.MediaStatus
import com.neki.photo.models.PhotoImage
import com.neki.user.models.User
import io.restassured.RestAssured
import io.restassured.http.ContentType
import org.assertj.core.api.Assertions.assertThat
import org.hamcrest.Matchers.equalTo
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.http.HttpStatus
import org.springframework.test.context.ActiveProfiles
import java.time.LocalDateTime

/**
 * fileName       : PutPhotoE2ETest
 * author         : codex
 * date           : 2026. 3. 15.
 * description    : PUT /api/photos/{photoId} E2E 테스트
 */
@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class PutPhotoE2ETest : PhotoImageE2ETestBase() {

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
    @DisplayName("기존 API와 동일한 LocalDateTime 포맷으로 capturedAt을 수정할 수 있다")
    fun givenStandardDateFormat_whenPutPhoto_thenUpdatesCapturedAt() {
        val media = createMedia(ownerId = testUser.id!!, status = MediaStatus.UPLOADED)
        val photo = createPhotoImage(userId = testUser.id!!, mediaId = media.id!!)
        val expectedCapturedAt = LocalDateTime.of(2025, 12, 23, 7, 9, 0)

        RestAssured.given()
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer $accessToken")
            .body(
                """
                {
                  "memo": "updated memo",
                  "capturedAt": "2025-12-23T07:09:00"
                }
                """.trimIndent(),
            )
            .`when`()
            .put("/api/photos/${photo.id}")
            .then()
            .statusCode(HttpStatus.OK.value())
            .body("resultCode", equalTo(ResultCode.SUCCESS.code))

        val updatedPhoto = photoImageRepository.findById(photo.id!!).orElseThrow()
        assertThat(updatedPhoto.memo).isEqualTo("updated memo")
        assertThat(updatedPhoto.capturedAt).isEqualTo(expectedCapturedAt)
    }

    @Test
    @DisplayName("memo와 capturedAt에 null을 보내면 둘 다 null로 저장된다")
    fun givenNullValues_whenPutPhoto_thenStoresNulls() {
        val media = createMedia(ownerId = testUser.id!!, status = MediaStatus.UPLOADED)
        val photo = photoImageRepository.save(
            PhotoImage(
                userId = testUser.id!!,
                mediaId = media.id!!,
                memo = "old memo",
                capturedAt = LocalDateTime.of(2024, 1, 2, 3, 4, 5),
            ),
        )

        RestAssured.given()
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer $accessToken")
            .body(
                """
                {
                  "memo": null,
                  "capturedAt": null
                }
                """.trimIndent(),
            )
            .`when`()
            .put("/api/photos/${photo.id}")
            .then()
            .statusCode(HttpStatus.OK.value())
            .body("resultCode", equalTo(ResultCode.SUCCESS.code))

        val updatedPhoto = photoImageRepository.findById(photo.id!!).orElseThrow()
        assertThat(updatedPhoto.memo).isNull()
        assertThat(updatedPhoto.capturedAt).isNull()
    }

    @Test
    @DisplayName("blank memo는 그대로 저장되고 capturedAt은 null로 처리된다")
    fun givenBlankMemoAndNullCapturedAt_whenPutPhoto_thenStoresRequestedValues() {
        val media = createMedia(ownerId = testUser.id!!, status = MediaStatus.UPLOADED)
        val photo = photoImageRepository.save(
            PhotoImage(
                userId = testUser.id!!,
                mediaId = media.id!!,
                memo = "old memo",
                capturedAt = LocalDateTime.of(2024, 6, 1, 10, 20, 30),
            ),
        )

        RestAssured.given()
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer $accessToken")
            .body(
                """
                {
                  "memo": "",
                  "capturedAt": null
                }
                """.trimIndent(),
            )
            .`when`()
            .put("/api/photos/${photo.id}")
            .then()
            .statusCode(HttpStatus.OK.value())
            .body("resultCode", equalTo(ResultCode.SUCCESS.code))

        val updatedPhoto = photoImageRepository.findById(photo.id!!).orElseThrow()
        assertThat(updatedPhoto.memo).isEqualTo("")
        assertThat(updatedPhoto.capturedAt).isNull()
    }
}
