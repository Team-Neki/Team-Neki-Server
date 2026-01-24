package com.yapp2app.e2e.media

import com.yapp2app.common.api.dto.ResultCode
import com.yapp2app.media.api.dto.UploadTicketRequest
import com.yapp2app.media.domain.MediaType
import com.yapp2app.media.domain.entity.MediaStatus
import com.yapp2app.user.domain.entity.User
import io.restassured.RestAssured
import io.restassured.http.ContentType
import org.assertj.core.api.Assertions.assertThat
import org.hamcrest.Matchers.equalTo
import org.hamcrest.Matchers.hasSize
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
 * date           : 2026. 1. 23.
 * description    : POST /api/media E2E 테스트
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
    @DisplayName("5개의 upload ticket 발급 성공 - 모든 Media가 INITIATED 상태로 생성된다")
    fun givenFiveItems_whenGenerateUploadTicket_thenReturnsFiveTicketsAndMediaCreated() {
        // given
        val request = UploadTicketRequest(
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
                UploadTicketRequest.UploadTicketItem(
                    filename = "photo4.png",
                    contentType = "image/png",
                    mediaType = MediaType.PHOTO_BOOTH,
                ),
                UploadTicketRequest.UploadTicketItem(
                    filename = "photo5.jpg",
                    contentType = "image/jpeg",
                    mediaType = MediaType.PHOTO_BOOTH,
                ),
            ),
        )

        // when
        val mediaIds = RestAssured.given()
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer $accessToken")
            .body(request)
            .`when`()
            .post("/api/media")
            .then()
            .statusCode(HttpStatus.OK.value())
            .body("resultCode", equalTo(ResultCode.SUCCESS.code))
            .body("data.tickets", hasSize<Any>(5))
            .body("data.tickets[0].mediaId", notNullValue())
            .body("data.tickets[0].uploadTicket", notNullValue())
            .body("data.tickets[0].method", equalTo("PUT"))
            .body("data.tickets[0].expiresIn", notNullValue())
            .body("data.tickets[0].contentType", notNullValue())
            .extract()
            .jsonPath()
            .getList<Int>("data.tickets.mediaId")
            .map { it.toLong() }

        // then - 5개의 Media가 모두 INITIATED 상태로 생성되었는지 확인
        assertThat(mediaIds).hasSize(5)
        mediaIds.forEach { mediaId ->
            val media = mediaRepository.findById(mediaId).orElseThrow()
            assertThat(media.status).isEqualTo(MediaStatus.INITIATED)
            assertThat(media.ownerId).isEqualTo(testUser.id)
            assertThat(media.mediaType).isEqualTo(MediaType.PHOTO_BOOTH)
        }
    }

    @Test
    @DisplayName("최대 10개의 upload ticket 발급 성공")
    fun givenTenItems_whenGenerateUploadTicket_thenReturnsTenTickets() {
        // given
        val request = UploadTicketRequest(
            items = (1..10).map {
                UploadTicketRequest.UploadTicketItem(
                    filename = "photo$it.jpg",
                    contentType = "image/jpeg",
                    mediaType = MediaType.PHOTO_BOOTH,
                )
            },
        )

        // when & then
        val mediaIds = RestAssured.given()
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer $accessToken")
            .body(request)
            .`when`()
            .post("/api/media")
            .then()
            .statusCode(HttpStatus.OK.value())
            .body("resultCode", equalTo(ResultCode.SUCCESS.code))
            .body("data.tickets", hasSize<Any>(10))
            .extract()
            .jsonPath()
            .getList<Int>("data.tickets.mediaId")
            .map { it.toLong() }

        // then
        assertThat(mediaIds).hasSize(10)
    }

    @Test
    @DisplayName("검증 실패 - 빈 리스트")
    fun givenEmptyItems_whenGenerateUploadTicket_thenReturnsBadRequest() {
        // given
        val request = UploadTicketRequest(items = emptyList())

        // when & then
        RestAssured.given()
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer $accessToken")
            .body(request)
            .`when`()
            .post("/api/media")
            .then()
            .statusCode(HttpStatus.BAD_REQUEST.value())
    }

    @Test
    @DisplayName("검증 실패 - 11개 초과")
    fun givenElevenItems_whenGenerateUploadTicket_thenReturnsBadRequest() {
        // given
        val request = UploadTicketRequest(
            items = (1..11).map {
                UploadTicketRequest.UploadTicketItem(
                    filename = "photo$it.jpg",
                    contentType = "image/jpeg",
                    mediaType = MediaType.PHOTO_BOOTH,
                )
            },
        )

        // when & then
        RestAssured.given()
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer $accessToken")
            .body(request)
            .`when`()
            .post("/api/media")
            .then()
            .statusCode(HttpStatus.BAD_REQUEST.value())
    }

    @Test
    @DisplayName("검증 실패 - filename이 빈 문자열")
    fun givenEmptyFilename_whenGenerateUploadTicket_thenReturnsBadRequest() {
        // given
        val request = UploadTicketRequest(
            items = listOf(
                UploadTicketRequest.UploadTicketItem(
                    filename = "",
                    contentType = "image/jpeg",
                    mediaType = MediaType.PHOTO_BOOTH,
                ),
            ),
        )

        // when & then
        RestAssured.given()
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer $accessToken")
            .body(request)
            .`when`()
            .post("/api/media")
            .then()
            .statusCode(HttpStatus.BAD_REQUEST.value())
            .body("resultCode", equalTo(ResultCode.INVALID_PARAMETER.code))
    }

    @Test
    @DisplayName("검증 실패 - contentType이 빈 문자열")
    fun givenEmptyContentType_whenGenerateUploadTicket_thenReturnsBadRequest() {
        // given
        val request = UploadTicketRequest(
            items = listOf(
                UploadTicketRequest.UploadTicketItem(
                    filename = "test.jpg",
                    contentType = "",
                    mediaType = MediaType.PHOTO_BOOTH,
                ),
            ),
        )

        // when & then
        RestAssured.given()
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer $accessToken")
            .body(request)
            .`when`()
            .post("/api/media")
            .then()
            .statusCode(HttpStatus.BAD_REQUEST.value())
            .body("resultCode", equalTo(ResultCode.INVALID_PARAMETER.code))
    }

    @Test
    @DisplayName("검증 실패 - mediaType이 null")
    fun givenNullMediaType_whenGenerateUploadTicket_thenReturnsBadRequest() {
        // given
        val request = UploadTicketRequest(
            items = listOf(
                UploadTicketRequest.UploadTicketItem(
                    filename = "test.jpg",
                    contentType = "image/jpeg",
                    mediaType = null,
                ),
            ),
        )

        // when & then
        RestAssured.given()
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer $accessToken")
            .body(request)
            .`when`()
            .post("/api/media")
            .then()
            .statusCode(HttpStatus.BAD_REQUEST.value())
            .body("resultCode", equalTo(ResultCode.INVALID_PARAMETER.code))
    }

    @Test
    @DisplayName("인증되지 않은 사용자는 403 에러를 반환한다")
    fun givenNoAuth_whenGenerateUploadTicket_thenReturnsForbidden() {
        // given
        val request = UploadTicketRequest(
            items = listOf(
                UploadTicketRequest.UploadTicketItem(
                    filename = "test.jpg",
                    contentType = "image/jpeg",
                    mediaType = MediaType.PHOTO_BOOTH,
                ),
            ),
        )

        // when & then
        RestAssured.given()
            .contentType(ContentType.JSON)
            .body(request)
            .`when`()
            .post("/api/media")
            .then()
            .statusCode(HttpStatus.FORBIDDEN.value())
    }

    @Test
    @DisplayName("응답에 uploadTicket 필드가 포함되어야 한다 (uploadUrl이 아님)")
    fun givenValidRequest_whenGenerateUploadTicket_thenResponseContainsUploadTicketField() {
        // given
        val request = UploadTicketRequest(
            items = listOf(
                UploadTicketRequest.UploadTicketItem(
                    filename = "test.jpg",
                    contentType = "image/jpeg",
                    mediaType = MediaType.PHOTO_BOOTH,
                ),
            ),
        )

        // when & then
        RestAssured.given()
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer $accessToken")
            .body(request)
            .`when`()
            .post("/api/media")
            .then()
            .statusCode(HttpStatus.OK.value())
            .body("data.tickets[0].uploadTicket", notNullValue())
    }
}
