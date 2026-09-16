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
 * fileName       : SearchPhotoBoothsByKeywordE2ETest
 * author         : koo
 * date           : 2026. 9. 15.
 * description    : GET /api/search/completion/photo-booths E2E 테스트 (mock 응답)
 */
@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class SearchPhotoBoothsByKeywordE2ETest : E2ETestBase() {

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
        .get("/api/search/completion/photo-booths")
        .then()

    @Nested
    @DisplayName("성공 케이스")
    inner class SuccessTests {

        @Test
        @DisplayName("사용자 위치 없음 - 지점명 접두 일치로 찾고 브랜드, 지점 이름 순이다")
        fun givenNoUserLocation_whenSearch_thenOrderedByBrandAndBranch() {
            get("keyword" to "강남")
                .statusCode(HttpStatus.OK.value())
                .body("resultCode", equalTo(ResultCode.SUCCESS.code))
                .body("data.totalCount", equalTo(5))
                .body("data.hasNext", equalTo(false))
                .body("data.items[0].keyword", equalTo("인생네컷 강남2호점"))
                .body("data.items[1].keyword", equalTo("인생네컷 강남역점"))
                .body("data.items[2].keyword", equalTo("포토이즘 강남1호점"))
                .body("data.items[3].keyword", equalTo("포토이즘 강남2호점"))
                .body("data.items[4].keyword", equalTo("포토이즘 강남역점"))
        }

        @Test
        @DisplayName("사용자 위치 있음 - 가까운 순으로 정렬된다")
        fun givenUserLocation_whenSearch_thenReturnsBoothsOrderedByDistance() {
            get("keyword" to "강남", "latitude" to 37.4979, "longitude" to 127.0276)
                .statusCode(HttpStatus.OK.value())
                .body("data.totalCount", equalTo(5))
                .body("data.items[0].keyword", equalTo("포토이즘 강남역점"))
                .body("data.items[4].keyword", equalTo("포토이즘 강남1호점"))
        }

        @Test
        @DisplayName("지점명 접두가 아니면 걸리지 않는다")
        fun givenNonPrefixKeyword_whenSearch_thenReturnsEmptyList() {
            get("keyword" to "호점")
                .statusCode(HttpStatus.OK.value())
                .body("resultCode", equalTo(ResultCode.SUCCESS.code))
                .body("data.items", empty<Any>())
                .body("data.totalCount", equalTo(0))
        }

        @Test
        @DisplayName("브랜드명은 검색 대상이 아니다")
        fun givenBrandName_whenSearch_thenReturnsEmptyList() {
            get("keyword" to "포토이즘")
                .statusCode(HttpStatus.OK.value())
                .body("data.items", empty<Any>())
                .body("data.totalCount", equalTo(0))
        }

        @Test
        @DisplayName("주소는 검색 대상이 아니다")
        fun givenAddress_whenSearch_thenReturnsEmptyList() {
            get("keyword" to "서울")
                .statusCode(HttpStatus.OK.value())
                .body("data.items", empty<Any>())
                .body("data.totalCount", equalTo(0))
        }

        @Test
        @DisplayName("keyword 는 브랜드 이름과 지점 이름을 띄어 쓴 값이다")
        fun givenKeyword_whenSearch_thenReturnsBrandAndBranchName() {
            get("keyword" to "역삼")
                .statusCode(HttpStatus.OK.value())
                .body("data.totalCount", equalTo(1))
                .body("data.items[0].keyword", equalTo("포토이즘 역삼점"))
        }

        @Test
        @DisplayName("좌표 경계값 - 위도 90, 경도 180 은 유효하다")
        fun givenBoundaryCoordinates_whenSearch_thenReturnsOk() {
            get("keyword" to "강남", "latitude" to 90.0, "longitude" to 180.0)
                .statusCode(HttpStatus.OK.value())
                .body("resultCode", equalTo(ResultCode.SUCCESS.code))
                .body("data.totalCount", equalTo(5))
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
                .body("data.items[0].keyword", equalTo("포토이즘 강남역점"))
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
        @DisplayName("위도만 보냄 - D-01")
        fun givenLatitudeOnly_whenSearch_thenReturnsInvalidParameter() {
            get("keyword" to "강남", "latitude" to 37.4979)
                .statusCode(HttpStatus.BAD_REQUEST.value())
                .body("resultCode", equalTo(ResultCode.INVALID_PARAMETER.code))
        }

        @Test
        @DisplayName("경도만 보냄 - D-01")
        fun givenLongitudeOnly_whenSearch_thenReturnsInvalidParameter() {
            get("keyword" to "강남", "longitude" to 127.0276)
                .statusCode(HttpStatus.BAD_REQUEST.value())
                .body("resultCode", equalTo(ResultCode.INVALID_PARAMETER.code))
        }

        @Test
        @DisplayName("위도가 범위를 벗어남 - D-01")
        fun givenLatitudeOutOfRange_whenSearch_thenReturnsInvalidParameter() {
            get("keyword" to "강남", "latitude" to 90.1, "longitude" to 127.0276)
                .statusCode(HttpStatus.BAD_REQUEST.value())
                .body("resultCode", equalTo(ResultCode.INVALID_PARAMETER.code))
        }

        @Test
        @DisplayName("경도가 범위를 벗어남 - D-01")
        fun givenLongitudeOutOfRange_whenSearch_thenReturnsInvalidParameter() {
            get("keyword" to "강남", "latitude" to 37.4979, "longitude" to -180.1)
                .statusCode(HttpStatus.BAD_REQUEST.value())
                .body("resultCode", equalTo(ResultCode.INVALID_PARAMETER.code))
        }

        @Test
        @DisplayName("좌표가 NaN - D-01")
        fun givenNaNCoordinate_whenSearch_thenReturnsInvalidParameter() {
            get("keyword" to "강남", "latitude" to "NaN", "longitude" to "127.0276")
                .statusCode(HttpStatus.BAD_REQUEST.value())
                .body("resultCode", equalTo(ResultCode.INVALID_PARAMETER.code))
        }

        @Test
        @DisplayName("좌표가 Infinity - D-01")
        fun givenInfiniteCoordinate_whenSearch_thenReturnsInvalidParameter() {
            get("keyword" to "강남", "latitude" to "37.4979", "longitude" to "Infinity")
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
                .get("/api/search/completion/photo-booths")
                .then()
                .statusCode(HttpStatus.FORBIDDEN.value())
                .body("resultCode", equalTo(ResultCode.MISSING_TOKEN_ERROR.code))
        }
    }
}
