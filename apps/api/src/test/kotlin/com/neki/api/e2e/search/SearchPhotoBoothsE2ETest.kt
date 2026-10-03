package com.neki.api.e2e.search

import com.neki.api.search.api.dto.SearchRequest
import com.neki.core.code.ResultCode
import com.neki.domain.map.models.PhotoBoothLocation
import io.restassured.RestAssured
import io.restassured.http.ContentType
import org.assertj.core.api.Assertions.assertThat
import org.hamcrest.Matchers.contains
import org.hamcrest.Matchers.empty
import org.hamcrest.Matchers.equalTo
import org.hamcrest.Matchers.everyItem
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
 * description    : POST /api/search/photo-booths E2E 테스트
 */
@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class SearchPhotoBoothsE2ETest : SearchE2ETestBase() {

    @LocalServerPort
    private var port: Int = 0

    private lateinit var accessToken: String

    private lateinit var booths: GangnamBooths

    private val gangnamLocation = SearchRequest.UserLocation(latitude = 37.4979, longitude = 127.0276)

    @BeforeEach
    fun setUp() {
        RestAssured.port = port
        RestAssured.baseURI = "http://localhost"

        val (user, token) = createTestUserAndToken()
        accessToken = token
        booths = GangnamBooths(user.id!!)
    }

    private fun post(keyword: String?, body: Any = SearchRequest.GetPhotoBooths(SearchRequest.FilterGroup())) =
        RestAssured.given()
            .header("Authorization", "Bearer $accessToken")
            .contentType(ContentType.JSON)
            .apply { if (keyword != null) queryParam("keyword", keyword) }
            .body(body)
            .`when`()
            .post("/api/search/photo-booths")
            .then()

    private fun brandFilter(vararg brandIds: Long) = SearchRequest.FilterGroup(
        brandFilter = SearchRequest.FilterGroup.BrandFilter(
            brands = brandIds.map { SearchRequest.FilterGroup.BrandFilter.Brand(brandId = it) },
        ),
    )

    @Nested
    @DisplayName("성공 케이스")
    inner class SuccessTests {

        @Test
        @DisplayName("지역 + 사용자 위치 - 지도 부스 id 로 가까운 순, 숨김·미동기화 지점은 빠진다")
        fun givenRegionAndUserLocation_whenSearch_thenReturnsVisibleBoothsOrderedByDistance() {
            val response = post(
                "서울특별시 강남구",
                SearchRequest.GetPhotoBooths(SearchRequest.FilterGroup(), userLocation = gangnamLocation),
            )
                .statusCode(HttpStatus.OK.value())
                .body("resultCode", equalTo(ResultCode.SUCCESS.code))
                .body(
                    "data.items.id",
                    contains(
                        booths.photoismGangnamStation.id!!.toInt(),
                        booths.lifeFourCutGangnamStation.id!!.toInt(),
                        booths.photoismGangnam1.id!!.toInt(),
                    ),
                )
                .body("data.items.favorite", contains(false, false, true))
                .body("data.items[2].brandName", equalTo("포토이즘"))
                .body("data.items[2].branchName", equalTo("강남1호점"))
                .extract()

            val distances: List<Int> = response.jsonPath().getList("data.items.distance", Integer::class.java).map {
                it.toInt()
            }
            assertThat(distances).isSorted().allMatch { it in 1..1000 }
        }

        @Test
        @DisplayName("사용자 위치 + 거리가 같은 부스 - 지점 이름 순으로 정렬된다")
        fun givenBoothsAtSameDistance_whenSearch_thenOrderedByBranchName() {
            // 포토이즘 강남역점과 같은 자리에 먼저 저장된 것보다 이름이 앞서는 지점을 둔다
            val sameSpot: PhotoBoothLocation = createMapBooth(
                createIndexedBooth(
                    booths.lifeFourCut,
                    "l3",
                    "가로수점",
                    127.0289042,
                    37.4967118,
                    listOf("1100000000", "1168000000"),
                ),
            )

            post(
                "서울특별시 강남구",
                SearchRequest.GetPhotoBooths(SearchRequest.FilterGroup(), userLocation = gangnamLocation),
            )
                .statusCode(HttpStatus.OK.value())
                .body("data.items[0].id", equalTo(sameSpot.id!!.toInt()))
                .body("data.items[1].id", equalTo(booths.photoismGangnamStation.id!!.toInt()))
        }

        @Test
        @DisplayName("사용자 위치 없음 - distance 가 null 이고 브랜드, 지점 이름 순으로 정렬된다")
        fun givenNoUserLocation_whenSearch_thenDistanceIsNullAndOrderedByBrandAndBranch() {
            post("서울특별시 강남구")
                .statusCode(HttpStatus.OK.value())
                .body(
                    "data.items.id",
                    contains(
                        booths.lifeFourCutGangnamStation.id!!.toInt(),
                        booths.photoismGangnam1.id!!.toInt(),
                        booths.photoismGangnamStation.id!!.toInt(),
                    ),
                )
                .body("data.items.distance", everyItem(nullValue()))
        }

        @Test
        @DisplayName("역 선택 - 역에 딸린 부스 목록을 반환한다 (다른 구의 부스 포함)")
        fun givenStation_whenSearch_thenReturnsBoothsOfStation() {
            post("강남역 2호선")
                .statusCode(HttpStatus.OK.value())
                .body(
                    "data.items.id",
                    contains(
                        booths.lifeFourCutGangnamStation.id!!.toInt(),
                        booths.lifeFourCutSeocho.id!!.toInt(),
                        booths.photoismGangnam1.id!!.toInt(),
                        booths.photoismGangnamStation.id!!.toInt(),
                    ),
                )
        }

        @Test
        @DisplayName("브랜드 필터 - 해당 브랜드의 부스만 반환한다")
        fun givenBrandFilter_whenSearch_thenReturnsOnlyThatBrand() {
            post("강남역 2호선", SearchRequest.GetPhotoBooths(brandFilter(booths.lifeFourCut.id!!)))
                .statusCode(HttpStatus.OK.value())
                .body(
                    "data.items.id",
                    contains(booths.lifeFourCutGangnamStation.id!!.toInt(), booths.lifeFourCutSeocho.id!!.toInt()),
                )
        }

        @Test
        @DisplayName("keyword 앞뒤·연속 공백 - 한 칸으로 맞춰 같은 지역으로 찾는다")
        fun givenKeywordWithExtraSpaces_whenSearch_thenNormalizesSpaces() {
            post("  서울특별시   강남구 ")
                .statusCode(HttpStatus.OK.value())
                .body("data.items.size()", equalTo(3))
        }

        @Test
        @DisplayName("자치구 + 브랜드 검색어 - 순서와 무관하게 그 자치구의 그 브랜드만 반환한다")
        fun givenDistrictAndBrandKeyword_whenSearch_thenReturnsThatBrandInDistrict() {
            val expected = contains(booths.photoismGangnam1.id!!.toInt(), booths.photoismGangnamStation.id!!.toInt())

            post("강남구 포토이즘").statusCode(HttpStatus.OK.value()).body("data.items.id", expected)
            post("포토이즘 강남").statusCode(HttpStatus.OK.value()).body("data.items.id", expected)
        }

        @Test
        @DisplayName("브랜드 + 역 검색어 - 역 반경 안의 그 브랜드만 반환한다 (다른 구 포함)")
        fun givenBrandAndStationKeyword_whenSearch_thenReturnsThatBrandNearStation() {
            post("인생네컷 강남역")
                .statusCode(HttpStatus.OK.value())
                .body(
                    "data.items.id",
                    contains(booths.lifeFourCutGangnamStation.id!!.toInt(), booths.lifeFourCutSeocho.id!!.toInt()),
                )
        }

        @Test
        @DisplayName("검색어의 브랜드와 브랜드 필터가 겹치지 않음 - 빈 배열을 반환한다")
        fun givenKeywordBrandOutsideBrandFilter_whenSearch_thenReturnsEmptyList() {
            post("강남구 포토이즘", SearchRequest.GetPhotoBooths(brandFilter(booths.lifeFourCut.id!!)))
                .statusCode(HttpStatus.OK.value())
                .body("data.items", empty<Any>())
        }

        @Test
        @DisplayName("지역·역을 찾지 못함 - 에러가 아니라 빈 배열 (없는 지역, 서울 밖 자치구, 브랜드만, 시도)")
        fun givenKeywordWithoutArea_whenSearch_thenReturnsEmptyList() {
            listOf("서울특별시 없는구", "부산진구 포토이즘", "포토이즘", "서울특별시").forEach { keyword ->
                post(keyword)
                    .statusCode(HttpStatus.OK.value())
                    .body("resultCode", equalTo(ResultCode.SUCCESS.code))
                    .body("data.items", empty<Any>())
            }
        }

        @Test
        @DisplayName("부스가 없는 역 - 빈 배열을 반환한다")
        fun givenStationWithoutBooths_whenSearch_thenReturnsEmptyList() {
            post("강남구청역 7호선")
                .statusCode(HttpStatus.OK.value())
                .body("resultCode", equalTo(ResultCode.SUCCESS.code))
                .body("data.items", empty<Any>())
        }
    }

    @Nested
    @DisplayName("실패 케이스")
    inner class FailureTests {

        @Test
        @DisplayName("공백뿐인 keyword - D-01")
        fun givenBlankKeyword_whenSearch_thenReturnsInvalidParameter() {
            post(" ")
                .statusCode(HttpStatus.BAD_REQUEST.value())
                .body("resultCode", equalTo(ResultCode.INVALID_PARAMETER.code))
        }

        @Test
        @DisplayName("keyword 없음 - D-01")
        fun givenNoKeyword_whenSearch_thenReturnsInvalidParameter() {
            post(null)
                .statusCode(HttpStatus.BAD_REQUEST.value())
                .body("resultCode", equalTo(ResultCode.INVALID_PARAMETER.code))
        }

        @Test
        @DisplayName("filterGroup 없음 - D-01")
        fun givenNoFilterGroup_whenSearch_thenReturnsInvalidParameter() {
            post("서울특별시 강남구", body = "{}")
                .statusCode(HttpStatus.BAD_REQUEST.value())
                .body("resultCode", equalTo(ResultCode.INVALID_PARAMETER.code))
        }

        @Test
        @DisplayName("토큰 없음 - 403")
        fun givenNoToken_whenSearch_thenReturnsForbidden() {
            RestAssured.given()
                .contentType(ContentType.JSON)
                .queryParam("keyword", "서울특별시 강남구")
                .body(SearchRequest.GetPhotoBooths(SearchRequest.FilterGroup()))
                .`when`()
                .post("/api/search/photo-booths")
                .then()
                .statusCode(HttpStatus.FORBIDDEN.value())
                .body("resultCode", equalTo(ResultCode.MISSING_TOKEN_ERROR.code))
        }
    }
}
