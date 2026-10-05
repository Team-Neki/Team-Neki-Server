package com.neki.api.e2e.search

import com.neki.api.search.api.dto.SearchRequest
import com.neki.core.code.ResultCode
import io.restassured.RestAssured
import io.restassured.http.ContentType
import org.assertj.core.api.Assertions.assertThat
import org.hamcrest.Matchers.contains
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
 * fileName       : GetSearchFilterE2ETest
 * author         : koo
 * date           : 2026. 9. 15.
 * description    : POST /api/search/filter E2E 테스트
 */
@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class GetSearchFilterE2ETest : SearchE2ETestBase() {

    @LocalServerPort
    private var port: Int = 0

    private lateinit var accessToken: String

    private var userId: Long = 0

    private lateinit var booths: GangnamBooths

    @BeforeEach
    fun setUp() {
        RestAssured.port = port
        RestAssured.baseURI = "http://localhost"

        val (user, token) = createTestUserAndToken()
        accessToken = token
        userId = user.id!!
        booths = GangnamBooths(userId)
    }

    private fun post(keyword: String, filterGroup: SearchRequest.FilterGroup = SearchRequest.FilterGroup()) =
        RestAssured.given()
            .header("Authorization", "Bearer $accessToken")
            .contentType(ContentType.JSON)
            .body(SearchRequest.GetFilter(keyword, filterGroup))
            .`when`()
            .post("/api/search/filter")
            .then()

    private fun postPhotoBooths(keyword: String, filterGroup: SearchRequest.FilterGroup) = RestAssured.given()
        .header("Authorization", "Bearer $accessToken")
        .contentType(ContentType.JSON)
        .body(SearchRequest.GetPhotoBooths(keyword, filterGroup))
        .`when`()
        .post("/api/search/photo-booths")
        .then()

    @Nested
    @DisplayName("성공 케이스")
    inner class SuccessTests {

        @Test
        @DisplayName("지역 선택 - 목록에 있는 부스만 브랜드별로 세고, 사용자 순서가 없으면 브랜드 ID 순이다")
        fun givenRegion_whenGetFilter_thenCountsVisibleBoothsByBrand() {
            post("서울특별시 강남구")
                .statusCode(HttpStatus.OK.value())
                .body("resultCode", equalTo(ResultCode.SUCCESS.code))
                .body("data.brandFilter.code", contains("PHOTOISM", "LIFEFOURCUTS"))
                .body("data.brandFilter.count", contains(2, 1))
                .body("data.brandFilter[0].id", equalTo(booths.photoism.id!!.toInt()))
                .body("data.brandFilter[0].name", equalTo("포토이즘"))
        }

        @Test
        @DisplayName("사용자별 브랜드 순서 - 그 순서대로 정렬된다")
        fun givenUserBrandOrder_whenGetFilter_thenFollowsUserOrder() {
            orderBrands(userId, booths.lifeFourCut, booths.photoism)

            post("서울특별시 강남구")
                .statusCode(HttpStatus.OK.value())
                .body("data.brandFilter.code", contains("LIFEFOURCUTS", "PHOTOISM"))
        }

        @Test
        @DisplayName("역 선택 + 브랜드 필터 - 그 브랜드만 집계한다")
        fun givenStationAndBrandFilter_whenGetFilter_thenCountsOnlyThatBrand() {
            val filterGroup = SearchRequest.FilterGroup(
                brandFilter = SearchRequest.FilterGroup.BrandFilter(
                    brands = listOf(SearchRequest.FilterGroup.BrandFilter.Brand(brandId = booths.lifeFourCut.id!!)),
                ),
            )

            post("강남역 2호선", filterGroup)
                .statusCode(HttpStatus.OK.value())
                .body("data.brandFilter.code", contains("LIFEFOURCUTS"))
                .body("data.brandFilter.count", contains(2))
        }

        @Test
        @DisplayName("검색어에 브랜드가 있음 - 그 브랜드만 집계한다")
        fun givenBrandInKeyword_whenGetFilter_thenCountsOnlyThatBrand() {
            post("포토이즘 강남역 2호선")
                .statusCode(HttpStatus.OK.value())
                .body("data.brandFilter.code", contains("PHOTOISM"))
                .body("data.brandFilter.count", contains(2))
        }

        @Test
        @DisplayName("부스 keyword - 그 지점의 브랜드 하나를 1개로 세고, 다른 브랜드 필터면 빈 배열이다")
        fun givenBoothKeyword_whenGetFilter_thenCountsThatBoothOnly() {
            val lifeFourCutOnly = SearchRequest.FilterGroup(
                brandFilter = SearchRequest.FilterGroup.BrandFilter(
                    brands = listOf(SearchRequest.FilterGroup.BrandFilter.Brand(brandId = booths.lifeFourCut.id!!)),
                ),
            )

            post("포토이즘 강남역점")
                .statusCode(HttpStatus.OK.value())
                .body("data.brandFilter.code", contains("PHOTOISM"))
                .body("data.brandFilter.count", contains(1))
            post("포토이즘 강남역점", lifeFourCutOnly)
                .statusCode(HttpStatus.OK.value())
                .body("data.brandFilter", empty<Any>())
        }

        @Test
        @DisplayName("같은 keyword·필터면 브랜드 개수 합계가 부스 목록 건수와 같다 (숨김·미동기화 지점은 양쪽에서 빠진다)")
        fun givenSameRequest_whenGetFilterAndPhotoBooths_thenCountsMatchList() {
            val photoismOnly = SearchRequest.FilterGroup(
                brandFilter = SearchRequest.FilterGroup.BrandFilter(
                    brands = listOf(SearchRequest.FilterGroup.BrandFilter.Brand(brandId = booths.photoism.id!!)),
                ),
            )

            listOf(
                "서울특별시 강남구" to SearchRequest.FilterGroup(),
                "강남역 2호선" to SearchRequest.FilterGroup(),
                "강남역 2호선" to photoismOnly,
                "인생네컷 강남역 2호선" to SearchRequest.FilterGroup(),
                "포토이즘 강남역점" to SearchRequest.FilterGroup(),
                "포토이즘 강남역점" to photoismOnly,
            ).forEach { (keyword, filterGroup) ->
                val counts: List<Int> = post(keyword, filterGroup)
                    .statusCode(HttpStatus.OK.value())
                    .extract()
                    .jsonPath()
                    .getList("data.brandFilter.count", Integer::class.java)
                    .map { it.toInt() }
                val items: Int = postPhotoBooths(keyword, filterGroup)
                    .statusCode(HttpStatus.OK.value())
                    .extract()
                    .jsonPath()
                    .getList<Any>("data.items")
                    .size

                assertThat(counts.sum()).`as`("$keyword $filterGroup").isEqualTo(items).isPositive()
            }
        }

        @Test
        @DisplayName("지역·역을 찾지 못함 - 에러가 아니라 빈 배열")
        fun givenKeywordWithoutArea_whenGetFilter_thenReturnsEmptyList() {
            post("없는역 2호선")
                .statusCode(HttpStatus.OK.value())
                .body("resultCode", equalTo(ResultCode.SUCCESS.code))
                .body("data.brandFilter", empty<Any>())
        }

        @Test
        @DisplayName("부스가 없는 역 - 빈 배열을 반환한다")
        fun givenStationWithoutBooths_whenGetFilter_thenReturnsEmptyList() {
            post("강남구청역 7호선")
                .statusCode(HttpStatus.OK.value())
                .body("data.brandFilter", empty<Any>())
        }
    }

    @Nested
    @DisplayName("실패 케이스")
    inner class FailureTests {

        @Test
        @DisplayName("공백뿐인 keyword - D-01")
        fun givenBlankKeyword_whenGetFilter_thenReturnsInvalidParameter() {
            post(" ")
                .statusCode(HttpStatus.BAD_REQUEST.value())
                .body("resultCode", equalTo(ResultCode.INVALID_PARAMETER.code))
        }

        @Test
        @DisplayName("토큰 없음 - 403")
        fun givenNoToken_whenGetFilter_thenReturnsForbidden() {
            RestAssured.given()
                .contentType(ContentType.JSON)
                .body(SearchRequest.GetFilter("서울특별시 강남구", SearchRequest.FilterGroup()))
                .`when`()
                .post("/api/search/filter")
                .then()
                .statusCode(HttpStatus.FORBIDDEN.value())
                .body("resultCode", equalTo(ResultCode.MISSING_TOKEN_ERROR.code))
        }
    }
}
