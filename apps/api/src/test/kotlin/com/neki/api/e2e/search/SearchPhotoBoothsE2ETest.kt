package com.neki.api.e2e.search

import com.neki.api.e2e.E2ETestBase
import com.neki.api.search.api.dto.SearchRequest
import com.neki.core.code.ResultCode
import io.restassured.RestAssured
import io.restassured.http.ContentType
import org.assertj.core.api.Assertions.assertThat
import org.hamcrest.Matchers.empty
import org.hamcrest.Matchers.equalTo
import org.hamcrest.Matchers.everyItem
import org.hamcrest.Matchers.hasSize
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
 * fileName       : SearchPhotoBoothsE2ETest
 * author         : koo
 * date           : 2026. 9. 15.
 * description    : POST /api/search/photo-booths E2E 테스트 (mock 응답)
 */
@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class SearchPhotoBoothsE2ETest : E2ETestBase() {

    @LocalServerPort
    private var port: Int = 0

    private lateinit var accessToken: String

    private val gangnamGu = SearchRequest.FilterGroup.RegionFilter(code = "1168000000")
    private val gangnamStation = SearchRequest.FilterGroup.StationFilter(name = "강남", lineName = "2호선")
    private val gangnamLocation = SearchRequest.FilterGroup.UserLocation(latitude = 37.4979, longitude = 127.0276)

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
        .post("/api/search/photo-booths")
        .then()

    @Nested
    @DisplayName("성공 케이스")
    inner class SuccessTests {

        @Test
        @DisplayName("지역 + 사용자 위치 - distance 가 채워지고 가까운 순으로 정렬된다")
        fun givenRegionAndUserLocation_whenSearch_thenReturnsBoothsOrderedByDistance() {
            val response = post(SearchRequest.FilterGroup(regionFilter = gangnamGu, userLocation = gangnamLocation))
                .statusCode(HttpStatus.OK.value())
                .body("resultCode", equalTo(ResultCode.SUCCESS.code))
                .body("data.items", hasSize<Int>(4))
                .body("data.items[0].id", equalTo(2591))
                .body("data.items[0].favorite", equalTo(false))
                .body("data.items[3].id", equalTo(2560))
                .body("data.items[3].favorite", equalTo(true))
                .extract()

            val distances: List<Int> = response.jsonPath().getList("data.items.distance", Integer::class.java).map {
                it.toInt()
            }
            assertThat(distances).isSorted()
        }

        @Test
        @DisplayName("사용자 위치 없음 - distance 가 null 이고 브랜드, 지점 이름 순으로 정렬된다")
        fun givenNoUserLocation_whenSearch_thenDistanceIsNullAndOrderedByBrandAndBranch() {
            post(SearchRequest.FilterGroup(regionFilter = gangnamGu))
                .statusCode(HttpStatus.OK.value())
                .body("data.items", hasSize<Int>(4))
                .body("data.items.distance", everyItem(nullValue()))
                .body("data.items[0].brandName", equalTo("인생네컷"))
                .body("data.items[1].id", equalTo(2560))
                .body("data.items[2].id", equalTo(2591))
                .body("data.items[3].id", equalTo(2604))
        }

        @Test
        @DisplayName("역 선택 - 역에 딸린 부스 목록을 반환한다")
        fun givenStation_whenSearch_thenReturnsBoothsOfStation() {
            post(SearchRequest.FilterGroup(stationFilter = gangnamStation, userLocation = gangnamLocation))
                .statusCode(HttpStatus.OK.value())
                .body("resultCode", equalTo(ResultCode.SUCCESS.code))
                .body("data.items", hasSize<Int>(6))
                .body("data.items[0].latitude", equalTo(37.4967118f))
                .body("data.items[0].longitude", equalTo(127.0289042f))
        }

        @Test
        @DisplayName("브랜드 필터 - 해당 브랜드의 부스만 반환한다")
        fun givenBrandIds_whenSearch_thenReturnsOnlyThatBrand() {
            post(
                SearchRequest.FilterGroup(
                    regionFilter = gangnamGu,
                    brandFilter = SearchRequest.FilterGroup.BrandFilter(brandIds = listOf(2)),
                ),
            )
                .statusCode(HttpStatus.OK.value())
                .body("data.items", hasSize<Int>(1))
                .body("data.items[0].brandCode", equalTo("LIFEFOURCUTS"))
        }

        @Test
        @DisplayName("부스가 없는 지역 - 빈 배열을 반환한다")
        fun givenRegionWithoutBooths_whenSearch_thenReturnsEmptyList() {
            post(SearchRequest.FilterGroup(regionFilter = SearchRequest.FilterGroup.RegionFilter(code = "4817010300")))
                .statusCode(HttpStatus.OK.value())
                .body("resultCode", equalTo(ResultCode.SUCCESS.code))
                .body("data.items", empty<Any>())
        }
    }

    @Nested
    @DisplayName("실패 케이스")
    inner class FailureTests {

        @Test
        @DisplayName("없는 지역 - D-04")
        fun givenUnknownRegion_whenSearch_thenReturnsNotFound() {
            post(SearchRequest.FilterGroup(regionFilter = SearchRequest.FilterGroup.RegionFilter(code = "9999999999")))
                .statusCode(HttpStatus.BAD_REQUEST.value())
                .body("resultCode", equalTo(ResultCode.NOT_FOUND.code))
        }

        @Test
        @DisplayName("없는 역 - D-04")
        fun givenUnknownStation_whenSearch_thenReturnsNotFound() {
            post(
                SearchRequest.FilterGroup(
                    stationFilter = SearchRequest.FilterGroup.StationFilter(name = "강남", lineName = "9호선"),
                ),
            )
                .statusCode(HttpStatus.BAD_REQUEST.value())
                .body("resultCode", equalTo(ResultCode.NOT_FOUND.code))
        }

        @Test
        @DisplayName("지역과 역을 둘 다 보냄 - D-01")
        fun givenBothRegionAndStation_whenSearch_thenReturnsInvalidParameter() {
            post(SearchRequest.FilterGroup(regionFilter = gangnamGu, stationFilter = gangnamStation))
                .statusCode(HttpStatus.BAD_REQUEST.value())
                .body("resultCode", equalTo(ResultCode.INVALID_PARAMETER.code))
        }

        @Test
        @DisplayName("지역과 역을 둘 다 안 보냄 - D-01")
        fun givenNeitherRegionNorStation_whenSearch_thenReturnsInvalidParameter() {
            post(SearchRequest.FilterGroup())
                .statusCode(HttpStatus.BAD_REQUEST.value())
                .body("resultCode", equalTo(ResultCode.INVALID_PARAMETER.code))
        }

        @Test
        @DisplayName("지역 코드가 빈 문자열 - D-01")
        fun givenBlankRegionCode_whenSearch_thenReturnsInvalidParameter() {
            post(SearchRequest.FilterGroup(regionFilter = SearchRequest.FilterGroup.RegionFilter(code = " ")))
                .statusCode(HttpStatus.BAD_REQUEST.value())
                .body("resultCode", equalTo(ResultCode.INVALID_PARAMETER.code))
        }

        @Test
        @DisplayName("토큰 없음 - 403")
        fun givenNoToken_whenSearch_thenReturnsForbidden() {
            RestAssured.given()
                .contentType(ContentType.JSON)
                .body(SearchRequest.FilterGroup(regionFilter = gangnamGu))
                .`when`()
                .post("/api/search/photo-booths")
                .then()
                .statusCode(HttpStatus.FORBIDDEN.value())
                .body("resultCode", equalTo(ResultCode.MISSING_TOKEN_ERROR.code))
        }
    }
}
