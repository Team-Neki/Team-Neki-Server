package com.neki.api.e2e.search

import com.neki.api.e2e.E2ETestBase
import com.neki.api.search.api.dto.SearchRequest
import com.neki.core.code.ResultCode
import io.restassured.RestAssured
import io.restassured.http.ContentType
import org.hamcrest.Matchers.empty
import org.hamcrest.Matchers.equalTo
import org.hamcrest.Matchers.hasSize
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
 * description    : POST /api/search/filter E2E 테스트 (mock 응답)
 */
@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class GetSearchFilterE2ETest : E2ETestBase() {

    @LocalServerPort
    private var port: Int = 0

    private lateinit var accessToken: String

    private val gangnamGu = SearchRequest.FilterGroup.RegionFilter(code = "1168000000")
    private val gangnamStation = SearchRequest.FilterGroup.StationFilter(name = "강남", lineName = "신분당선")

    @BeforeEach
    fun setUp() {
        RestAssured.port = port
        RestAssured.baseURI = "http://localhost"

        val (_, token) = createTestUserAndToken()
        accessToken = token
    }

    private fun post(request: SearchRequest.FilterGroup) = RestAssured.given()
        .header("Authorization", "Bearer $accessToken")
        .contentType(ContentType.JSON)
        .body(request)
        .`when`()
        .post("/api/search/filter")
        .then()

    @Nested
    @DisplayName("성공 케이스")
    inner class SuccessTests {

        @Test
        @DisplayName("지역 선택 - 목록에 있는 브랜드만 부스 개수와 함께 브랜드 ID 순으로 반환한다")
        fun givenRegion_whenGetFilter_thenReturnsBrandsWithCount() {
            post(SearchRequest.FilterGroup(regionFilter = gangnamGu))
                .statusCode(HttpStatus.OK.value())
                .body("resultCode", equalTo(ResultCode.SUCCESS.code))
                .body("data.brandFilter", hasSize<Int>(2))
                .body("data.brandFilter[0].id", equalTo(1))
                .body("data.brandFilter[0].code", equalTo("PHOTOISM"))
                .body("data.brandFilter[0].count", equalTo(3))
                .body("data.brandFilter[1].id", equalTo(2))
                .body("data.brandFilter[1].code", equalTo("LIFEFOURCUTS"))
                .body("data.brandFilter[1].count", equalTo(1))
        }

        @Test
        @DisplayName("역 선택 + userLocation - userLocation 은 무시되고 역에 딸린 부스로 집계한다")
        fun givenStationWithUserLocation_whenGetFilter_thenIgnoresUserLocation() {
            post(
                SearchRequest.FilterGroup(
                    stationFilter = gangnamStation,
                    userLocation = SearchRequest.FilterGroup.UserLocation(latitude = 37.4979, longitude = 127.0276),
                ),
            )
                .statusCode(HttpStatus.OK.value())
                .body("data.brandFilter", hasSize<Int>(2))
                .body("data.brandFilter[0].count", equalTo(4))
                .body("data.brandFilter[1].count", equalTo(2))
        }

        @Test
        @DisplayName("브랜드 필터 - 그 브랜드만 집계한다")
        fun givenBrandIds_whenGetFilter_thenCountsOnlyThatBrand() {
            post(
                SearchRequest.FilterGroup(
                    regionFilter = gangnamGu,
                    brandFilter = SearchRequest.FilterGroup.BrandFilter(brandIds = listOf(1)),
                ),
            )
                .statusCode(HttpStatus.OK.value())
                .body("data.brandFilter", hasSize<Int>(1))
                .body("data.brandFilter[0].id", equalTo(1))
                .body("data.brandFilter[0].count", equalTo(3))
        }

        @Test
        @DisplayName("부스가 없는 지역 - 빈 배열을 반환한다")
        fun givenRegionWithoutBooths_whenGetFilter_thenReturnsEmptyList() {
            post(SearchRequest.FilterGroup(regionFilter = SearchRequest.FilterGroup.RegionFilter(code = "5279033026")))
                .statusCode(HttpStatus.OK.value())
                .body("data.brandFilter", empty<Any>())
        }
    }

    @Nested
    @DisplayName("실패 케이스")
    inner class FailureTests {

        @Test
        @DisplayName("없는 역 - D-04")
        fun givenUnknownStation_whenGetFilter_thenReturnsNotFound() {
            post(
                SearchRequest.FilterGroup(
                    stationFilter = SearchRequest.FilterGroup.StationFilter(name = "송정", lineName = "동해선"),
                ),
            )
                .statusCode(HttpStatus.BAD_REQUEST.value())
                .body("resultCode", equalTo(ResultCode.NOT_FOUND.code))
        }

        @Test
        @DisplayName("지역과 역을 둘 다 보냄 - D-01")
        fun givenBothRegionAndStation_whenGetFilter_thenReturnsInvalidParameter() {
            post(SearchRequest.FilterGroup(regionFilter = gangnamGu, stationFilter = gangnamStation))
                .statusCode(HttpStatus.BAD_REQUEST.value())
                .body("resultCode", equalTo(ResultCode.INVALID_PARAMETER.code))
        }

        @Test
        @DisplayName("지역과 역을 둘 다 안 보냄 - D-01")
        fun givenNeitherRegionNorStation_whenGetFilter_thenReturnsInvalidParameter() {
            post(SearchRequest.FilterGroup())
                .statusCode(HttpStatus.BAD_REQUEST.value())
                .body("resultCode", equalTo(ResultCode.INVALID_PARAMETER.code))
        }

        @Test
        @DisplayName("토큰 없음 - 403")
        fun givenNoToken_whenGetFilter_thenReturnsForbidden() {
            RestAssured.given()
                .contentType(ContentType.JSON)
                .body(SearchRequest.FilterGroup(regionFilter = gangnamGu))
                .`when`()
                .post("/api/search/filter")
                .then()
                .statusCode(HttpStatus.FORBIDDEN.value())
                .body("resultCode", equalTo(ResultCode.MISSING_TOKEN_ERROR.code))
        }
    }
}
