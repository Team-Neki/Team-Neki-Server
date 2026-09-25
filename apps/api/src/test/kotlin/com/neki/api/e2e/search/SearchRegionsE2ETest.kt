package com.neki.api.e2e.search

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
 * fileName       : SearchRegionsE2ETest
 * author         : darren
 * date           : 2026. 9. 25.
 * description    : GET /api/search/completion/regions E2E 테스트
 */
@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class SearchRegionsE2ETest : SearchE2ETestBase() {

    @LocalServerPort
    private var port: Int = 0

    private lateinit var accessToken: String

    @BeforeEach
    fun setUp() {
        RestAssured.port = port
        RestAssured.baseURI = "http://localhost"

        val (_, token) = createTestUserAndToken()
        accessToken = token

        // tb_legal_dong 의 실제 행. 시도·하위 계층·접두가 아닌 이름이 섞이도록 골랐다.
        createLegalDong("1100000000", 1, "서울특별시", "서울특별시")
        createLegalDong("1165000000", 2, "서초구", "서울특별시 서초구")
        createLegalDong("1165010100", 3, "방배동", "서울특별시 서초구 방배동")
        createLegalDong("1165010800", 3, "서초동", "서울특별시 서초구 서초동")
        createLegalDong("1168000000", 2, "강남구", "서울특별시 강남구")
        createLegalDong("1168010100", 3, "역삼동", "서울특별시 강남구 역삼동")
        createLegalDong("4817010300", 3, "강남동", "경상남도 진주시 강남동")
        createLegalDong("5279033026", 4, "강남리", "전북특별자치도 고창군 무장면 강남리")
    }

    private fun get(vararg params: Pair<String, Any>) = RestAssured.given()
        .header("Authorization", "Bearer $accessToken")
        .queryParams(params.toMap())
        .`when`()
        .get("/api/search/completion/regions")
        .then()

    @Nested
    @DisplayName("성공 케이스")
    inner class SuccessTests {

        @Test
        @DisplayName("접두 일치로 찾고 계층이 위인 것부터 전체 경로를 반환한다")
        fun givenKeyword_whenSearch_thenReturnsPrefixMatchedRegionsOrderedByLevel() {
            get("keyword" to "강남")
                .statusCode(HttpStatus.OK.value())
                .body("resultCode", equalTo(ResultCode.SUCCESS.code))
                .body("data.totalCount", equalTo(3))
                .body("data.hasNext", equalTo(false))
                .body("data.items[0].keyword", equalTo("서울특별시 강남구"))
                .body("data.items[1].keyword", equalTo("경상남도 진주시 강남동"))
                .body("data.items[2].keyword", equalTo("전북특별자치도 고창군 무장면 강남리"))
        }

        @Test
        @DisplayName("하위 계층 이름으로 검색하면 상위 계층은 빠진다")
        fun givenLeafName_whenSearch_thenReturnsOnlyThatLevel() {
            get("keyword" to "강남동")
                .statusCode(HttpStatus.OK.value())
                .body("data.totalCount", equalTo(1))
                .body("data.items[0].keyword", equalTo("경상남도 진주시 강남동"))
        }

        @Test
        @DisplayName("접두가 아니면 걸리지 않는다")
        fun givenNonPrefixKeyword_whenSearch_thenReturnsEmptyList() {
            get("keyword" to "남구")
                .statusCode(HttpStatus.OK.value())
                .body("resultCode", equalTo(ResultCode.SUCCESS.code))
                .body("data.items", empty<Any>())
                .body("data.totalCount", equalTo(0))
                .body("data.hasNext", equalTo(false))
        }

        @Test
        @DisplayName("시도는 검색 대상이 아니다")
        fun givenSidoName_whenSearch_thenReturnsEmptyList() {
            get("keyword" to "서울")
                .statusCode(HttpStatus.OK.value())
                .body("data.items", empty<Any>())
                .body("data.totalCount", equalTo(0))
        }

        @Test
        @DisplayName("1자 검색도 된다")
        fun givenSingleCharKeyword_whenSearch_thenReturnsMatches() {
            get("keyword" to "강")
                .statusCode(HttpStatus.OK.value())
                .body("data.totalCount", equalTo(3))
        }

        @Test
        @DisplayName("LIKE 와일드카드는 글자 그대로 찾는다")
        fun givenWildcardKeyword_whenSearch_thenMatchesLiterally() {
            get("keyword" to "%")
                .statusCode(HttpStatus.OK.value())
                .body("data.items", empty<Any>())
                .body("data.totalCount", equalTo(0))

            get("keyword" to "강_")
                .statusCode(HttpStatus.OK.value())
                .body("data.items", empty<Any>())
                .body("data.totalCount", equalTo(0))
        }

        @Test
        @DisplayName("검색어 앞뒤 공백은 무시한다")
        fun givenKeywordWithSurroundingSpaces_whenSearch_thenTrimsKeyword() {
            get("keyword" to " 강남동 ")
                .statusCode(HttpStatus.OK.value())
                .body("data.totalCount", equalTo(1))
                .body("data.items[0].keyword", equalTo("경상남도 진주시 강남동"))
        }

        @Test
        @DisplayName("페이징 - 첫 페이지는 hasNext 가 true 이고 totalCount 는 전체 건수다")
        fun givenFirstPage_whenSearch_thenHasNextIsTrueAndTotalCountIsWhole() {
            get("keyword" to "강남", "page" to 0, "size" to 2)
                .statusCode(HttpStatus.OK.value())
                .body("data.items.size()", equalTo(2))
                .body("data.hasNext", equalTo(true))
                .body("data.totalCount", equalTo(3))
                .body("data.items[0].keyword", equalTo("서울특별시 강남구"))
        }

        @Test
        @DisplayName("페이징 - 마지막 페이지는 hasNext 가 false 다")
        fun givenLastPage_whenSearch_thenHasNextIsFalse() {
            get("keyword" to "강남", "page" to 1, "size" to 2)
                .statusCode(HttpStatus.OK.value())
                .body("data.items.size()", equalTo(1))
                .body("data.hasNext", equalTo(false))
                .body("data.totalCount", equalTo(3))
                .body("data.items[0].keyword", equalTo("전북특별자치도 고창군 무장면 강남리"))
        }

        @Test
        @DisplayName("페이징 경계 - page * size 가 Int 최대값이면 아직 유효하고 빈 결과다")
        fun givenMaxOffsetPage_whenSearch_thenReturnsEmptyPage() {
            get("keyword" to "강남", "page" to Int.MAX_VALUE, "size" to 1)
                .statusCode(HttpStatus.OK.value())
                .body("resultCode", equalTo(ResultCode.SUCCESS.code))
                .body("data.items", empty<Any>())
                .body("data.hasNext", equalTo(false))
                .body("data.totalCount", equalTo(3))
        }

        @Test
        @DisplayName("부스 목록 API 가 아는 지역만 내려간다")
        fun givenSeochoKeyword_whenSearch_thenReturnsRegionsUsableForBoothList() {
            get("keyword" to "서초")
                .statusCode(HttpStatus.OK.value())
                .body("data.totalCount", equalTo(2))
                .body("data.items[0].keyword", equalTo("서울특별시 서초구"))
                .body("data.items[1].keyword", equalTo("서울특별시 서초구 서초동"))
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
        @DisplayName("검색어 없음 - D-01")
        fun givenNoKeyword_whenSearch_thenReturnsInvalidParameter() {
            get()
                .statusCode(HttpStatus.BAD_REQUEST.value())
                .body("resultCode", equalTo(ResultCode.INVALID_PARAMETER.code))
        }

        @Test
        @DisplayName("size 가 범위를 벗어남 - D-01")
        fun givenSizeOutOfRange_whenSearch_thenReturnsInvalidParameter() {
            get("keyword" to "강남", "size" to 101)
                .statusCode(HttpStatus.BAD_REQUEST.value())
                .body("resultCode", equalTo(ResultCode.INVALID_PARAMETER.code))
        }

        @Test
        @DisplayName("page 가 음수 - D-01")
        fun givenNegativePage_whenSearch_thenReturnsInvalidParameter() {
            get("keyword" to "강남", "page" to -1)
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
                .get("/api/search/completion/regions")
                .then()
                .statusCode(HttpStatus.FORBIDDEN.value())
                .body("resultCode", equalTo(ResultCode.MISSING_TOKEN_ERROR.code))
        }
    }
}
