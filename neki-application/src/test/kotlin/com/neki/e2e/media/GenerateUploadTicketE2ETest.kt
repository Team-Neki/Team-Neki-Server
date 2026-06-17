package com.neki.e2e.media

import com.neki.common.api.dto.ResultCode
import com.neki.media.MediaType
import com.neki.media.api.dto.UploadTicketRequest
import com.neki.user.entity.User
import io.restassured.RestAssured
import io.restassured.http.ContentType
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
        RestAssured.given()
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer $accessToken")
            .body(request)
            .`when`()
            .post("/api/media/upload")
            .then()
            .statusCode(HttpStatus.OK.value())
            .body("resultCode", equalTo(ResultCode.SUCCESS.code))
            .body("data.items", hasSize<Any>(5))
            .body("data.items[0].mediaId", notNullValue())
            .body("data.items[0].uploadTicket", notNullValue())
            .body("data.method", equalTo("PUT"))
            .body("data.expiresIn", notNullValue())
            .body("data.items[0].contentType", notNullValue())
            .extract()
            .jsonPath()
            .getList<Int>("data.items.mediaId")
            .map { it.toLong() }
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
        RestAssured.given()
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer $accessToken")
            .body(request)
            .`when`()
            .post("/api/media/upload")
            .then()
            .statusCode(HttpStatus.OK.value())
            .body("resultCode", equalTo(ResultCode.SUCCESS.code))
            .body("data.items", hasSize<Any>(10))
            .extract()
            .jsonPath()
            .getList<Int>("data.items.mediaId")
            .map { it.toLong() }
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
            .post("/api/media/upload")
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
            .post("/api/media/upload")
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
            .post("/api/media/upload")
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
            .post("/api/media/upload")
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
            .post("/api/media/upload")
            .then()
            .statusCode(HttpStatus.BAD_REQUEST.value())
            .body("resultCode", equalTo(ResultCode.INVALID_PARAMETER.code))
    }

    @Test
    @DisplayName("토큰이 없는 사용자는 403 에러를 반환한다")
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
            .post("/api/media/upload")
            .then()
            .statusCode(HttpStatus.FORBIDDEN.value())
    }
}
