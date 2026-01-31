package com.yapp2app.e2e.user

import com.yapp2app.common.api.dto.ResultCode
import com.yapp2app.e2e.media.MediaE2ETestBase
import com.yapp2app.media.api.dto.UploadTicketRequest
import com.yapp2app.media.domain.MediaType
import com.yapp2app.user.domain.entity.User
import io.restassured.RestAssured
import io.restassured.http.ContentType
import org.assertj.core.api.Assertions.assertThat
import org.hamcrest.CoreMatchers.equalTo
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.http.HttpStatus
import org.springframework.test.context.ActiveProfiles

/**
 * fileName       : UpdateUserProfileImageE2ETest
 * author         : claude
 * date           : 2026. 1. 31.
 * description    : 사용자 프로필 이미지 변경 E2E 테스트
 */
@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class UpdateUserProfileImageE2ETest : MediaE2ETestBase() {

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
    @DisplayName("프로필 이미지 변경 성공")
    fun givenValidMediaId_whenUpdateProfileImage_thenSuccess() {
        // given
        val mediaId = createUploadedMedia()

        // when & then
        updateProfileImage(mediaId)
            .statusCode(HttpStatus.OK.value())
            .body("resultCode", equalTo(ResultCode.SUCCESS.code))

        val updatedUser = userRepository.findById(testUser.id!!).orElseThrow()
        assertThat(updatedUser.profileImageId).isEqualTo(mediaId)
    }

    @Test
    @DisplayName("동일한 이미지로 중복 변경 시 멱등성 보장 - 이미지가 삭제되지 않음")
    fun givenSameMediaId_whenUpdateProfileImageTwice_thenIdempotent() {
        // given
        val mediaId = createUploadedMedia()

        // 1차 호출: 프로필 이미지 설정
        updateProfileImage(mediaId)
            .statusCode(HttpStatus.OK.value())

        // when: 2차 호출 (동일 mediaId)
        updateProfileImage(mediaId)
            .statusCode(HttpStatus.OK.value())

        // then: 프로필 이미지가 여전히 설정되어 있어야 함
        val updatedUser = userRepository.findById(testUser.id!!).orElseThrow()
        assertThat(updatedUser.profileImageId).isEqualTo(mediaId)

        // media가 삭제되지 않았는지 확인
        val media = mediaRepository.findById(mediaId)
        assertThat(media).isPresent
    }

    @Test
    @DisplayName("기본 이미지로 변경 성공")
    fun givenProfileImageSet_whenUpdateToDefault_thenSuccess() {
        // given: 프로필 이미지 설정
        val mediaId = createUploadedMedia()
        updateProfileImage(mediaId)
            .statusCode(HttpStatus.OK.value())

        // when: 기본 이미지로 변경 (mediaId = null)
        updateProfileImage(null)
            .statusCode(HttpStatus.OK.value())

        // then
        val updatedUser = userRepository.findById(testUser.id!!).orElseThrow()
        assertThat(updatedUser.profileImageId).isNull()
    }

    @Test
    @DisplayName("이미 기본 이미지인 상태에서 기본 이미지 변경 시 멱등성 보장")
    fun givenDefaultImage_whenUpdateToDefault_thenIdempotent() {
        // given: 기본 이미지 상태 (profileImageId = null)
        val user = userRepository.findById(testUser.id!!).orElseThrow()
        assertThat(user.profileImageId).isNull()

        // when: 기본 이미지로 변경 (이미 기본 이미지)
        updateProfileImage(null)
            .statusCode(HttpStatus.OK.value())

        // then
        val updatedUser = userRepository.findById(testUser.id!!).orElseThrow()
        assertThat(updatedUser.profileImageId).isNull()
    }

    @Test
    @DisplayName("존재하지 않는 mediaId로 변경 시 에러 반환")
    fun givenNonExistentMediaId_whenUpdateProfileImage_thenNotFound() {
        // when & then
        updateProfileImage(999999L)
            .statusCode(HttpStatus.BAD_REQUEST.value())
            .body("resultCode", equalTo(ResultCode.NOT_FOUND.code))
    }

    /**
     * 업로드 ticket 발급을 통해 UPLOADED 상태의 media를 생성하고 mediaId를 반환
     */
    private fun createUploadedMedia(): Long {
        val ticketRequest = UploadTicketRequest(
            items = listOf(
                UploadTicketRequest.UploadTicketItem(
                    filename = "profile-${System.currentTimeMillis()}.jpg",
                    contentType = "image/jpeg",
                    mediaType = MediaType.USER_PROFILE,
                ),
            ),
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
            .first()
    }

    private fun updateProfileImage(mediaId: Long?): io.restassured.response.ValidatableResponse {
        val body = if (mediaId != null) {
            mapOf("mediaId" to mediaId)
        } else {
            mapOf("mediaId" to null)
        }

        return RestAssured.given()
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer $accessToken")
            .body(body)
            .`when`()
            .patch("/api/users/me/profile-image")
            .then()
    }
}
