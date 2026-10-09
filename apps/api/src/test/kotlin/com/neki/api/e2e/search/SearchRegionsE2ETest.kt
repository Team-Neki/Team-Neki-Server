package com.neki.api.e2e.search

import com.neki.core.code.ResultCode
import com.neki.domain.map.models.Brand
import io.restassured.RestAssured
import org.hamcrest.Matchers.contains
import org.hamcrest.Matchers.empty
import org.hamcrest.Matchers.equalTo
import org.hamcrest.Matchers.nullValue
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

    private lateinit var planB: Brand

    @BeforeEach
    fun setUp() {
        RestAssured.port = port
        RestAssured.baseURI = "http://localhost"

        val (_, token) = createTestUserAndToken()
        accessToken = token

        // tb_legal_dong 의 실제 행. 시도·하위 계층·접두가 아닌 이름이 섞이도록 골랐다.
        val photoGray: Brand = createBrand("포토그레이", "PHOTOGRAY")
        planB = createBrand("플랜비 스튜디오", "PLANB_STUDIO")

        createLegalDong("1100000000", 1, "서울특별시", "서울특별시")
        createLegalDong("1165000000", 2, "서초구", "서울특별시 서초구")
        createLegalDong("1165010100", 3, "방배동", "서울특별시 서초구 방배동")
        createLegalDong("1165010800", 3, "서초동", "서울특별시 서초구 서초동")
        createLegalDong("1168000000", 2, "강남구", "서울특별시 강남구")
        createLegalDong("1168010100", 3, "역삼동", "서울특별시 강남구 역삼동")
        createLegalDong("4817010300", 3, "강남동", "경상남도 진주시 강남동")
        createLegalDong("5279033026", 4, "강남리", "전북특별자치도 고창군 무장면 강남리")
        createLegalDong("4111100000", 2, "수원시 장안구", "경기도 수원시 장안구")
        createLegalDong("1230000000", 2, "북구", "전남광주통합특별시 북구")
        createLegalDong("4161000000", 2, "광주시", "경기도 광주시")
        createLegalDong("1150010300", 3, "화곡동", "서울특별시 강서구 화곡동")
        createLegalDong("4155039030", 4, "화곡리", "경기도 안성시 일죽면 화곡리")

        // 검색어의 브랜드 낱말 빼기와 filterGroup 은 검색 색인에 있는 브랜드(NER 사전)를 본다
        createIndexedBooth(photoGray, "pg1", "강남점", 127.0280, 37.4980, emptyList())
        createIndexedBooth(planB, "pb1", "강남점", 127.0290, 37.4990, emptyList())
        queryUnderstandingService.reloadDictionary()
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
                .body("data.items[0].distanceKm", nullValue())
                .body("data.items[1].keyword", equalTo("경상남도 진주시 강남동"))
                .body("data.items[2].keyword", equalTo("전북특별자치도 고창군 무장면 강남리"))
        }

        @Test
        @DisplayName("읍면동·리 이름 앞부분으로 찾는다")
        fun givenDongPrefix_whenSearch_thenReturnsDongAndRi() {
            get("keyword" to "화곡")
                .statusCode(HttpStatus.OK.value())
                .body("data.totalCount", equalTo(2))
                .body("data.items[0].keyword", equalTo("서울특별시 강서구 화곡동"))
                .body("data.items[1].keyword", equalTo("경기도 안성시 일죽면 화곡리"))
        }

        @Test
        @DisplayName("검색어에 브랜드가 없으면 기본 filterGroup 을 내려준다")
        fun givenKeywordWithoutBrand_whenSearch_thenReturnsDefaultFilterGroup() {
            get("keyword" to "강남")
                .statusCode(HttpStatus.OK.value())
                .body("data.filterGroup.brandFilter.brands", empty<Any>())
                .body("data.filterGroup.sortFilter.type", equalTo("DEFAULT"))
        }

        @Test
        @DisplayName("검색어에 브랜드가 있으면 지역 검색에서 뺀 그 브랜드를 건 filterGroup 을 내려준다")
        fun givenKeywordWithBrand_whenSearch_thenReturnsBrandFilterGroup() {
            listOf("강남 플랜비 스튜디오", "플랜비 스튜디오 서울특별시 강남").forEach { keyword ->
                get("keyword" to keyword)
                    .statusCode(HttpStatus.OK.value())
                    .body("data.filterGroup.brandFilter.brands.brandId", contains(planB.id!!.toInt()))
                    .body("data.filterGroup.sortFilter.type", equalTo("DEFAULT"))
            }
        }

        @Test
        @DisplayName("부스 탭과 같은 검색어에서 브랜드 낱말은 빼고 찾는다")
        fun givenKeywordWithBrandWord_whenSearch_thenIgnoresBrandWord() {
            listOf("강남 포토그레이", "포토그레이 강남", "강남 플랜비 스튜디오").forEach { keyword ->
                get("keyword" to keyword)
                    .statusCode(HttpStatus.OK.value())
                    .body("data.totalCount", equalTo(3))
                    .body("data.items[0].keyword", equalTo("서울특별시 강남구"))
            }

            get("keyword" to "포토그레이 서울 강남")
                .statusCode(HttpStatus.OK.value())
                .body("data.totalCount", equalTo(2))
                .body("data.items[0].keyword", equalTo("서울특별시 강남구"))
                .body("data.items[1].keyword", equalTo("서울특별시 강남구 역삼동"))
        }

        @Test
        @DisplayName("브랜드만 적으면 빈 결과다")
        fun givenBrandOnly_whenSearch_thenReturnsEmptyList() {
            listOf("포토그레이", "포토그레이 플랜비").forEach { keyword ->
                get("keyword" to keyword)
                    .statusCode(HttpStatus.OK.value())
                    .body("data.items", empty<Any>())
                    .body("data.totalCount", equalTo(0))
            }
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
        @DisplayName("시도만 치면 빈 결과다")
        fun givenSidoOnly_whenSearch_thenReturnsEmptyList() {
            listOf("서울", "서울특별시", "서울특별시 ", "서울시").forEach { keyword ->
                get("keyword" to keyword)
                    .statusCode(HttpStatus.OK.value())
                    .body("data.items", empty<Any>())
                    .body("data.totalCount", equalTo(0))
            }
        }

        @Test
        @DisplayName("시도 뒤에 공백과 한 글자만 이어 쳐도 그 아래 구역이 계층 순으로 나온다")
        fun givenSidoAndNextChar_whenSearch_thenReturnsRegionsUnderSido() {
            get("keyword" to "서울특별시 서")
                .statusCode(HttpStatus.OK.value())
                .body("data.totalCount", equalTo(3))
                .body("data.items[0].keyword", equalTo("서울특별시 서초구"))
                .body("data.items[1].keyword", equalTo("서울특별시 서초구 방배동"))
                .body("data.items[2].keyword", equalTo("서울특별시 서초구 서초동"))
        }

        @Test
        @DisplayName("1자 검색은 빈 결과다")
        fun givenSingleCharKeyword_whenSearch_thenReturnsEmptyList() {
            get("keyword" to "강")
                .statusCode(HttpStatus.OK.value())
                .body("resultCode", equalTo(ResultCode.SUCCESS.code))
                .body("data.items", empty<Any>())
                .body("data.totalCount", equalTo(0))
                .body("data.hasNext", equalTo(false))
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
        @DisplayName("전체 경로로 찾으면 그 구역과 하위 구역이 계층 순으로 나온다")
        fun givenFullPathKeyword_whenSearch_thenReturnsRegionAndDescendants() {
            get("keyword" to "서울특별시 강남구")
                .statusCode(HttpStatus.OK.value())
                .body("data.totalCount", equalTo(2))
                .body("data.items[0].keyword", equalTo("서울특별시 강남구"))
                .body("data.items[1].keyword", equalTo("서울특별시 강남구 역삼동"))
        }

        @Test
        @DisplayName("전체 경로의 마지막 이름은 접두만 적어도 된다")
        fun givenFullPathWithLeafPrefix_whenSearch_thenReturnsMatchedRegion() {
            get("keyword" to "서울특별시 강남")
                .statusCode(HttpStatus.OK.value())
                .body("data.totalCount", equalTo(2))
                .body("data.items[0].keyword", equalTo("서울특별시 강남구"))

            get("keyword" to "서울특별시 강남구 역")
                .statusCode(HttpStatus.OK.value())
                .body("data.totalCount", equalTo(1))
                .body("data.items[0].keyword", equalTo("서울특별시 강남구 역삼동"))
        }

        @Test
        @DisplayName("연속 공백은 한 칸으로 보고 찾는다")
        fun givenKeywordWithRepeatedSpaces_whenSearch_thenCollapsesSpaces() {
            get("keyword" to "서울특별시   강남구")
                .statusCode(HttpStatus.OK.value())
                .body("data.totalCount", equalTo(2))
                .body("data.items[0].keyword", equalTo("서울특별시 강남구"))
        }

        @Test
        @DisplayName("이름에 공백이 든 구역도 이름이나 전체 경로로 찾는다")
        fun givenLeafNameWithSpace_whenSearch_thenReturnsRegion() {
            get("keyword" to "수원시 장안")
                .statusCode(HttpStatus.OK.value())
                .body("data.totalCount", equalTo(1))
                .body("data.items[0].keyword", equalTo("경기도 수원시 장안구"))

            get("keyword" to "경기도 수원시 장안")
                .statusCode(HttpStatus.OK.value())
                .body("data.totalCount", equalTo(1))
                .body("data.items[0].keyword", equalTo("경기도 수원시 장안구"))
        }

        @Test
        @DisplayName("시도 줄임말로 전체 경로를 적어도 찾는다")
        fun givenSidoAlias_whenSearch_thenExpandsToFullSidoName() {
            get("keyword" to "서울 강남구")
                .statusCode(HttpStatus.OK.value())
                .body("data.totalCount", equalTo(2))
                .body("data.items[0].keyword", equalTo("서울특별시 강남구"))

            get("keyword" to "서울시 강남")
                .statusCode(HttpStatus.OK.value())
                .body("data.totalCount", equalTo(2))
                .body("data.items[0].keyword", equalTo("서울특별시 강남구"))
        }

        @Test
        @DisplayName("광주는 통합된 시도 이름으로 찾는다")
        fun givenGwangjuAlias_whenSearch_thenReturnsMergedSidoRegion() {
            get("keyword" to "광주 북구")
                .statusCode(HttpStatus.OK.value())
                .body("data.totalCount", equalTo(1))
                .body("data.items[0].keyword", equalTo("전남광주통합특별시 북구"))
        }

        @Test
        @DisplayName("줄임말 한 낱말은 바꾸지 않고 이름으로만 찾는다")
        fun givenAliasOnly_whenSearch_thenSearchesByNameOnly() {
            get("keyword" to "광주")
                .statusCode(HttpStatus.OK.value())
                .body("data.totalCount", equalTo(1))
                .body("data.items[0].keyword", equalTo("경기도 광주시"))
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
