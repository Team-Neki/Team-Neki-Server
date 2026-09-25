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

    private val noFilter = SearchRequest.FilterGroup()

    @BeforeEach
    fun setUp() {
        RestAssured.port = port
        RestAssured.baseURI = "http://localhost"

        val (_, token) = createTestUserAndToken()
        accessToken = token
    }

    private fun post(keyword: String?, request: Any = SearchRequest.GetFilter(filterGroup = noFilter)) =
        RestAssured.given()
            .header("Authorization", "Bearer $accessToken")
            .contentType(ContentType.JSON)
            .apply { keyword?.let { queryParam("keyword", it) } }
            .body(request)
            .`when`()
            .post("/api/search/filter")
            .then()

    @Nested
    @DisplayName("성공 케이스")
    inner class SuccessTests {

        @Test
        @DisplayName("검색어 - 고정된 브랜드 집계를 브랜드 ID 순으로 반환한다")
        fun givenKeyword_whenGetFilter_thenReturnsFixedBrandsWithCount() {
            post("강남")
                .statusCode(HttpStatus.OK.value())
                .body("resultCode", equalTo(ResultCode.SUCCESS.code))
                .body("data.brandFilter", hasSize<Int>(2))
                .body("data.brandFilter[0].id", equalTo(1))
                .body("data.brandFilter[0].code", equalTo("PHOTOISM"))
                .body("data.brandFilter[0].count", equalTo(4))
                .body("data.brandFilter[1].id", equalTo(2))
                .body("data.brandFilter[1].code", equalTo("LIFEFOURCUTS"))
                .body("data.brandFilter[1].count", equalTo(2))
        }

        @Test
        @DisplayName("검색어·브랜드 필터가 달라도 같은 응답을 반환한다")
        fun givenDifferentInputs_whenGetFilter_thenReturnsSameResponse() {
            val filterGroup = SearchRequest.FilterGroup(
                brandFilter = SearchRequest.FilterGroup.BrandFilter(
                    brands = listOf(SearchRequest.FilterGroup.BrandFilter.Brand(brandId = 2)),
                ),
            )

            val base: String = post("강남").statusCode(HttpStatus.OK.value()).extract().body().asString()
            val other: String = post("송정", SearchRequest.GetFilter(filterGroup = filterGroup))
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
        fun givenNoKeyword_whenGetFilter_thenReturnsInvalidParameter() {
            post(null)
                .statusCode(HttpStatus.BAD_REQUEST.value())
                .body("resultCode", equalTo(ResultCode.INVALID_PARAMETER.code))
        }

        @Test
        @DisplayName("keyword 가 공백 - D-01")
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
                .queryParam("keyword", "강남")
                .body(SearchRequest.GetFilter(filterGroup = noFilter))
                .`when`()
                .post("/api/search/filter")
                .then()
                .statusCode(HttpStatus.FORBIDDEN.value())
                .body("resultCode", equalTo(ResultCode.MISSING_TOKEN_ERROR.code))
        }
    }
}
