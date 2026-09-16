package com.neki.api.e2e.search

import com.neki.api.e2e.E2ETestBase
import com.neki.api.search.api.dto.SearchRequest
import com.neki.core.code.ResultCode
import io.restassured.RestAssured
import io.restassured.http.ContentType
import org.assertj.core.api.Assertions.assertThat
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

    private val noFilter = SearchRequest.FilterGroup()
    private val gangnamLocation = SearchRequest.UserLocation(latitude = 37.4979, longitude = 127.0276)

    @BeforeEach
    fun setUp() {
        RestAssured.port = port
        RestAssured.baseURI = "http://localhost"

        val (_, token) = createTestUserAndToken()
        accessToken = token
    }

    private fun post(keyword: String?, request: Any = SearchRequest.GetPhotoBooths(filterGroup = noFilter)) =
        RestAssured.given()
            .header("Authorization", "Bearer $accessToken")
            .contentType(ContentType.JSON)
            .apply { keyword?.let { queryParam("keyword", it) } }
            .body(request)
            .`when`()
            .post("/api/search/photo-booths")
            .then()

    @Nested
    @DisplayName("성공 케이스")
    inner class SuccessTests {

        @Test
        @DisplayName("검색어 + 사용자 위치 - 고정된 부스 6개를 가까운 순으로 반환한다")
        fun givenKeywordAndUserLocation_whenSearch_thenReturnsFixedBoothsOrderedByDistance() {
            post("강남", SearchRequest.GetPhotoBooths(filterGroup = noFilter, userLocation = gangnamLocation))
                .statusCode(HttpStatus.OK.value())
                .body("resultCode", equalTo(ResultCode.SUCCESS.code))
                .body("data.items", hasSize<Int>(6))
                .body("data.items.id", equalTo(listOf(2591, 3115, 3102, 2573, 2604, 2560)))
                .body("data.items.distance", equalTo(listOf(175, 250, 288, 360, 416, 469)))
                .body("data.items[0].latitude", equalTo(37.4967118f))
                .body("data.items[0].longitude", equalTo(127.0289042f))
                .body("data.items[5].favorite", equalTo(true))
        }

        @Test
        @DisplayName("검색어·브랜드 필터·정렬·사용자 위치가 달라도 같은 응답을 반환한다")
        fun givenDifferentInputs_whenSearch_thenReturnsSameResponse() {
            val filterGroup = SearchRequest.FilterGroup(
                brandFilter = SearchRequest.FilterGroup.BrandFilter(
                    brands = listOf(SearchRequest.FilterGroup.BrandFilter.Brand(brandId = 2)),
                ),
                sortFilter = SearchRequest.FilterGroup.SortFilter(
                    order = SearchRequest.FilterGroup.SortFilter.Order.DESC,
                ),
            )

            val base: String = post(
                "강남",
                SearchRequest.GetPhotoBooths(filterGroup = noFilter, userLocation = gangnamLocation),
            )
                .statusCode(HttpStatus.OK.value())
                .extract().body().asString()
            val other: String = post("송정", SearchRequest.GetPhotoBooths(filterGroup = filterGroup))
                .statusCode(HttpStatus.OK.value())
                .extract().body().asString()

            assertThat(other).isEqualTo(base)
        }
    }

    @Nested
    @DisplayName("실패 케이스")
    inner class FailureTests {

        @Test
        @DisplayName("keyword 없음 - D-01")
        fun givenNoKeyword_whenSearch_thenReturnsInvalidParameter() {
            post(null)
                .statusCode(HttpStatus.BAD_REQUEST.value())
                .body("resultCode", equalTo(ResultCode.INVALID_PARAMETER.code))
        }

        @Test
        @DisplayName("keyword 가 공백 - D-01")
        fun givenBlankKeyword_whenSearch_thenReturnsInvalidParameter() {
            post(" ")
                .statusCode(HttpStatus.BAD_REQUEST.value())
                .body("resultCode", equalTo(ResultCode.INVALID_PARAMETER.code))
        }

        @Test
        @DisplayName("filterGroup 없음 - D-01")
        fun givenNoFilterGroup_whenSearch_thenReturnsInvalidParameter() {
            post("강남", emptyMap<String, Any>())
                .statusCode(HttpStatus.BAD_REQUEST.value())
                .body("resultCode", equalTo(ResultCode.INVALID_PARAMETER.code))
        }

        @Test
        @DisplayName("사용자 위치 좌표가 범위를 벗어남 - D-01")
        fun givenOutOfRangeUserLocation_whenSearch_thenReturnsInvalidParameter() {
            post(
                "강남",
                SearchRequest.GetPhotoBooths(
                    filterGroup = noFilter,
                    userLocation = SearchRequest.UserLocation(latitude = 91.0, longitude = 127.0276),
                ),
            )
                .statusCode(HttpStatus.BAD_REQUEST.value())
                .body("resultCode", equalTo(ResultCode.INVALID_PARAMETER.code))
        }

        @Test
        @DisplayName("토큰 없음 - 403")
        fun givenNoToken_whenSearch_thenReturnsForbidden() {
            RestAssured.given()
                .contentType(ContentType.JSON)
                .queryParam("keyword", "강남")
                .body(SearchRequest.GetPhotoBooths(filterGroup = noFilter))
                .`when`()
                .post("/api/search/photo-booths")
                .then()
                .statusCode(HttpStatus.FORBIDDEN.value())
                .body("resultCode", equalTo(ResultCode.MISSING_TOKEN_ERROR.code))
        }
    }
}
