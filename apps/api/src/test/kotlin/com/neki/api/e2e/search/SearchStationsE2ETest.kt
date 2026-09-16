package com.neki.api.e2e.search

import com.neki.api.e2e.E2ETestBase
import com.neki.core.code.ResultCode
import io.restassured.RestAssured
import org.hamcrest.Matchers.empty
import org.hamcrest.Matchers.equalTo
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.http.HttpStatus
import org.springframework.test.context.ActiveProfiles

/**
 * fileName       : SearchStationsE2ETest
 * author         : koo
 * date           : 2026. 9. 15.
 * description    : GET /api/search/stations E2E 테스트 (mock 응답)
 */
@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class SearchStationsE2ETest : E2ETestBase() {

    @LocalServerPort
    private var port: Int = 0

    private lateinit var accessToken: String

    @BeforeEach
    fun setUp() {
        RestAssured.port = port
        RestAssured.baseURI = "http://localhost"

        val (_, token) = createTestUserAndToken()
        accessToken = token
    }

    private fun get(vararg params: Pair<String, Any>) = RestAssured.given()
        .header("Authorization", "Bearer $accessToken")
        .queryParams(params.toMap())
        .`when`()
        .get("/api/search/stations")
        .then()

    @Nested
    @DisplayName("성공 케이스")
    inner class SuccessTests {

        @Test
        @DisplayName("한 역이 노선 수만큼 나오고 역명, 노선명 순으로 정렬된다")
        fun givenKeyword_whenSearch_thenReturnsOneRowPerLine() {
            get("keyword" to "강남")
                .statusCode(HttpStatus.OK.value())
                .body("resultCode", equalTo(ResultCode.SUCCESS.code))
                .body("data.totalCount", equalTo(5))
                .body("data.hasNext", equalTo(false))
                .body("data.items[0].name", equalTo("강남"))
                .body("data.items[0].lineName", equalTo("2호선"))
                .body("data.items[1].name", equalTo("강남"))
                .body("data.items[1].lineName", equalTo("신분당선"))
                .body("data.items[2].name", equalTo("강남구청"))
                .body("data.items[2].lineName", equalTo("7호선"))
                .body("data.items[3].lineName", equalTo("분당선"))
                .body("data.items[4].name", equalTo("강남대"))
                .body("data.items[4].lineName", equalTo("에버라인"))
        }

        @Test
        @DisplayName("`역` 을 붙여 검색해도 같은 결과다")
        fun givenKeywordWithStationSuffix_whenSearch_thenReturnsSameResult() {
            get("keyword" to "강남역")
                .statusCode(HttpStatus.OK.value())
                .body("data.totalCount", equalTo(5))
                .body("data.items[0].name", equalTo("강남"))
        }

        @Test
        @DisplayName("접두가 아니면 걸리지 않는다")
        fun givenNonPrefixKeyword_whenSearch_thenReturnsEmptyList() {
            get("keyword" to "구청")
                .statusCode(HttpStatus.OK.value())
                .body("resultCode", equalTo(ResultCode.SUCCESS.code))
                .body("data.items", empty<Any>())
                .body("data.totalCount", equalTo(0))
        }

        @Test
        @DisplayName("역명을 다 입력하면 그 역의 노선만 나온다")
        fun givenFullStationName_whenSearch_thenReturnsOnlyItsLines() {
            get("keyword" to "강남구청")
                .statusCode(HttpStatus.OK.value())
                .body("data.totalCount", equalTo(2))
                .body("data.items[0].lineName", equalTo("7호선"))
                .body("data.items[1].lineName", equalTo("분당선"))
        }

        @Test
        @DisplayName("페이징 - 첫 페이지는 hasNext 가 true 이고 totalCount 는 전체 건수다")
        fun givenFirstPage_whenSearch_thenHasNextIsTrueAndTotalCountIsWhole() {
            get("keyword" to "강남", "page" to 0, "size" to 2)
                .statusCode(HttpStatus.OK.value())
                .body("data.items.size()", equalTo(2))
                .body("data.hasNext", equalTo(true))
                .body("data.totalCount", equalTo(5))
        }

        @Test
        @DisplayName("페이징 - 마지막 페이지는 hasNext 가 false 다")
        fun givenLastPage_whenSearch_thenHasNextIsFalse() {
            get("keyword" to "강남", "page" to 2, "size" to 2)
                .statusCode(HttpStatus.OK.value())
                .body("data.items.size()", equalTo(1))
                .body("data.hasNext", equalTo(false))
                .body("data.items[0].name", equalTo("강남대"))
        }
    }

    @Nested
    @DisplayName("실패 케이스")
    inner class FailureTests {

        @Test
        @DisplayName("공백뿐인 검색어 - D-01")
        fun givenBlankKeyword_whenSearch_thenReturnsInvalidParameter() {
            get("keyword" to " ")
                .statusCode(HttpStatus.BAD_REQUEST.value())
                .body("resultCode", equalTo(ResultCode.INVALID_PARAMETER.code))
        }

        @Test
        @DisplayName("size 가 범위를 벗어남 - D-01")
        fun givenSizeOutOfRange_whenSearch_thenReturnsInvalidParameter() {
            get("keyword" to "강남", "size" to 0)
                .statusCode(HttpStatus.BAD_REQUEST.value())
                .body("resultCode", equalTo(ResultCode.INVALID_PARAMETER.code))
        }

        @Test
        @DisplayName("page 가 너무 커서 page * size 가 Int 를 넘음 - D-14")
        fun givenOverflowingPage_whenSearch_thenReturnsInvalidPagination() {
            get("keyword" to "강남", "page" to Int.MAX_VALUE, "size" to 100)
                .statusCode(HttpStatus.BAD_REQUEST.value())
                .body("resultCode", equalTo(ResultCode.INVALID_PAGINATION.code))
        }

        @Test
        @DisplayName("토큰 없음 - 403")
        fun givenNoToken_whenSearch_thenReturnsForbidden() {
            RestAssured.given()
                .queryParam("keyword", "강남")
                .`when`()
                .get("/api/search/stations")
                .then()
                .statusCode(HttpStatus.FORBIDDEN.value())
                .body("resultCode", equalTo(ResultCode.MISSING_TOKEN_ERROR.code))
        }
    }
}
