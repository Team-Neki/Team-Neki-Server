package com.neki.api.e2e.map

import com.neki.api.map.api.dto.MapRequest
import com.neki.api.map.api.validation.CLOSED_POLYGON_MESSAGE
import com.neki.api.map.api.validation.MAX_POLYGON_POINTS
import com.neki.api.map.api.validation.MAX_POLYGON_POINTS_MESSAGE
import com.neki.core.api.dto.BaseResponse
import com.neki.core.code.ResultCode
import com.neki.domain.map.models.Brand
import com.neki.domain.user.models.User
import io.restassured.RestAssured
import io.restassured.http.ContentType
import org.assertj.core.api.Assertions.assertThat
import org.hamcrest.Matchers.equalTo
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.http.HttpStatus
import org.springframework.test.context.ActiveProfiles

/**
 * fileName       : GetFilterE2ETest
 * author         : darren
 * date           : 2026. 8. 23.
 * description    : 다각형 영역 내 브랜드 조회 E2E 테스트
 *
 * 다각형 조회는 PostGIS 함수(ST_Contains, ST_MakePolygon 등)에 의존하지만
 * 테스트 프로파일은 H2(PostgreSQL 모드)를 사용해 해당 함수를 제공하지 않는다.
 * 따라서 영역 필터링·정렬 검증 케이스는 기존 GetPhotoBoothsByPolygonE2ETest 와 동일하게 @Disabled 로 두고,
 * 실제 로직 검증은 GetFilterUseCaseTest(단위 테스트)가 담당한다.
 * 인증·검증 케이스는 쿼리에 도달하기 전에 처리되므로 여기서 활성 상태로 검증한다.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class GetFilterE2ETest : MapE2ETestBase() {

    @LocalServerPort
    private var port: Int = 0

    private lateinit var accessToken: String
    private lateinit var testUser: User
    private lateinit var testBrand: Brand

    // 강남역 기준 다각형 좌표
    private val gangnamPolygonCoordinates = listOf(
        MapRequest.GetPolygonLocation.Coordinate(127.019128, 37.502456),
        MapRequest.GetPolygonLocation.Coordinate(127.035359, 37.502853),
        MapRequest.GetPolygonLocation.Coordinate(127.035663, 37.494395),
        MapRequest.GetPolygonLocation.Coordinate(127.023675, 37.494257),
        MapRequest.GetPolygonLocation.Coordinate(127.019128, 37.502456),
    )

    @BeforeEach
    fun setUp() {
        RestAssured.port = port
        RestAssured.baseURI = "http://localhost"

        val (user, token) = createTestUserAndToken()
        testUser = user
        accessToken = token

        testBrand = createBrand("포토이즘", "PHOTOISM")
    }

    private fun filterGroupOf(
        coordinates: List<MapRequest.GetPolygonLocation.Coordinate>,
        brandIds: List<Long>? = null,
    ) = MapRequest.FilterGroup(
        polygonFilter = MapRequest.FilterGroup.PolygonFilter(coordinates = coordinates),
        brandFilter = MapRequest.FilterGroup.BrandFilter(brandIds = brandIds),
    )

    // ── 인증·검증 (PostGIS 불필요) ──────────────────────────────────────────────

    @Test
    @DisplayName("토큰 없이 조회 시 403 에러를 반환한다")
    fun givenNoToken_whenGetFilter_thenReturnsForbidden() {
        // Given
        val request = filterGroupOf(gangnamPolygonCoordinates)

        // When & Then
        RestAssured.given()
            .contentType(ContentType.JSON)
            .body(request)
            .`when`()
            .post("/api/photo-booths/polygon/filter")
            .then()
            .statusCode(HttpStatus.FORBIDDEN.value())
    }

    @Test
    @DisplayName("polygonFilter가 없으면 500이 아니라 검증 에러를 반환한다")
    fun givenNoPolygonFilter_whenGetFilter_thenReturnsBadRequest() {
        // Given: 필수 필터인 polygonFilter 를 누락 (역직렬화 단계에서 걸린다)
        val request = """{"brandFilter": {"brandIds": []}}"""

        // When & Then
        RestAssured.given()
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer $accessToken")
            .body(request)
            .`when`()
            .post("/api/photo-booths/polygon/filter")
            .then()
            .statusCode(HttpStatus.BAD_REQUEST.value())
            .body("message", equalTo(ResultCode.INVALID_PARAMETER.message))
    }

    @Test
    @DisplayName("brandFilter를 생략해도 역직렬화에 실패하지 않는다")
    fun givenNoBrandFilter_whenGetFilter_thenBodyIsStillDeserialized() {
        // Given: brandFilter 는 기본값이 있어 생략 가능하다. 폴리곤만 일부러 닫히지 않게 보낸다
        val coordinates: String = gangnamPolygonCoordinates.dropLast(1).joinToString(", ") {
            """{"longitude": ${it.longitude}, "latitude": ${it.latitude}}"""
        }
        val request = """{"polygonFilter": {"coordinates": [$coordinates]}}"""

        // When & Then: 역직렬화 실패(INVALID_PARAMETER)가 아니라 폴리곤 검증 메시지가 나와야 한다
        RestAssured.given()
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer $accessToken")
            .body(request)
            .`when`()
            .post("/api/photo-booths/polygon/filter")
            .then()
            .statusCode(HttpStatus.BAD_REQUEST.value())
            .body("message", equalTo(CLOSED_POLYGON_MESSAGE))
    }

    @Test
    @DisplayName("coordinates가 비어있으면 검증 에러를 반환한다")
    fun givenEmptyCoordinates_whenGetFilter_thenReturnsBadRequest() {
        // Given
        val request = filterGroupOf(emptyList())

        // When & Then
        RestAssured.given()
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer $accessToken")
            .body(request)
            .`when`()
            .post("/api/photo-booths/polygon/filter")
            .then()
            .statusCode(HttpStatus.BAD_REQUEST.value())
    }

    @Test
    @DisplayName("좌표에 경도가 없으면 검증 에러를 반환한다")
    fun givenCoordinateWithoutLongitude_whenGetFilter_thenReturnsBadRequest() {
        // Given
        val request = filterGroupOf(
            listOf(MapRequest.GetPolygonLocation.Coordinate(longitude = null, latitude = 37.502456)),
        )

        // When & Then
        RestAssured.given()
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer $accessToken")
            .body(request)
            .`when`()
            .post("/api/photo-booths/polygon/filter")
            .then()
            .statusCode(HttpStatus.BAD_REQUEST.value())
    }

    @Test
    @DisplayName("좌표가 4개 미만이면 500이 아니라 검증 에러를 반환한다")
    fun givenTooFewCoordinates_whenGetFilter_thenReturnsBadRequest() {
        // Given: 닫혀 있어도 3개면 PostGIS ST_MakePolygon 이 예외를 던진다
        val request = filterGroupOf(
            listOf(
                MapRequest.GetPolygonLocation.Coordinate(127.019128, 37.502456),
                MapRequest.GetPolygonLocation.Coordinate(127.035359, 37.502853),
                MapRequest.GetPolygonLocation.Coordinate(127.019128, 37.502456),
            ),
        )

        // When & Then
        RestAssured.given()
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer $accessToken")
            .body(request)
            .`when`()
            .post("/api/photo-booths/polygon/filter")
            .then()
            .statusCode(HttpStatus.BAD_REQUEST.value())
            .body("message", equalTo(CLOSED_POLYGON_MESSAGE))
    }

    @Test
    @DisplayName("첫 좌표와 마지막 좌표가 다르면 500이 아니라 검증 에러를 반환한다")
    fun givenUnclosedPolygon_whenGetFilter_thenReturnsBadRequest() {
        // Given: 마지막 좌표를 빼 다각형이 닫히지 않은 상태
        val request = filterGroupOf(gangnamPolygonCoordinates.dropLast(1))

        // When & Then
        RestAssured.given()
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer $accessToken")
            .body(request)
            .`when`()
            .post("/api/photo-booths/polygon/filter")
            .then()
            .statusCode(HttpStatus.BAD_REQUEST.value())
            .body("message", equalTo(CLOSED_POLYGON_MESSAGE))
    }

    @Test
    @DisplayName("좌표가 상한을 넘으면 검증 에러를 반환한다")
    fun givenTooManyCoordinates_whenGetFilter_thenReturnsBadRequest() {
        // Given: 닫혀 있고 4개 이상이지만 개수 상한을 1개 초과 → Size 제약만 위반한다
        val tooManyCoordinates = (0 until MAX_POLYGON_POINTS).map {
            MapRequest.GetPolygonLocation.Coordinate(127.0 + it * 0.00001, 37.0 + it * 0.00001)
        } + MapRequest.GetPolygonLocation.Coordinate(127.0, 37.0)

        val request = filterGroupOf(tooManyCoordinates)

        // When & Then
        RestAssured.given()
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer $accessToken")
            .body(request)
            .`when`()
            .post("/api/photo-booths/polygon/filter")
            .then()
            .statusCode(HttpStatus.BAD_REQUEST.value())
            .body("message", equalTo(MAX_POLYGON_POINTS_MESSAGE))
    }

    // ── 조회 동작 (PostGIS 필요) ────────────────────────────────────────────────

    @Test
    @Disabled("PostGIS 함수 미지원(H2)으로 테스트 불가. 로직은 GetFilterUseCaseTest 에서 검증")
    @DisplayName("다각형 영역 내에 포토부스가 없으면 빈 목록을 반환한다")
    fun givenNoPhotoBooths_whenGetFilter_thenReturnsEmptyList() {
        // Given: 포토부스가 없는 상태
        val request = filterGroupOf(gangnamPolygonCoordinates)

        // When
        val response = RestAssured.given()
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer $accessToken")
            .body(request)
            .`when`()
            .post("/api/photo-booths/polygon/filter")
            .then()
            .extract()

        // Then
        assertThat(response.statusCode()).isEqualTo(HttpStatus.OK.value())

        val baseResponse = response.`as`(BaseResponse::class.java)
        assertThat(baseResponse.resultCode).isEqualTo(ResultCode.SUCCESS.code)
        assertThat(response.jsonPath().getList<Any>("data.brandFilter")).isEmpty()
    }

    @Test
    @Disabled("PostGIS 함수 미지원(H2)으로 테스트 불가. 로직은 GetFilterUseCaseTest 에서 검증")
    @DisplayName("영역 내에 포토부스가 있는 브랜드만 반환한다")
    fun givenPhotoBoothsInPolygon_whenGetFilter_thenReturnsOnlyBrandsInArea() {
        // Given: 영역 내에는 testBrand, 영역 밖(부산)에는 anotherBrand 의 포토부스만 존재
        val anotherBrand = createBrand("인생네컷", "LIFEFOURCUTS")

        createPhotoBoothLocation(
            brandId = testBrand.id!!,
            name = "포토이즘 강남점",
            address = "서울 강남구 강남대로 396",
            longitude = 127.027456,
            latitude = 37.497946,
        )
        createPhotoBoothLocation(
            brandId = anotherBrand.id!!,
            name = "인생네컷 해운대점",
            address = "부산 해운대구",
            longitude = 129.160480,
            latitude = 35.163574,
        )

        val request = filterGroupOf(gangnamPolygonCoordinates)

        // When
        val response = RestAssured.given()
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer $accessToken")
            .body(request)
            .`when`()
            .post("/api/photo-booths/polygon/filter")
            .then()
            .extract()

        // Then: 영역 내 브랜드만 반환
        assertThat(response.statusCode()).isEqualTo(HttpStatus.OK.value())
        assertThat(response.jsonPath().getList<Long>("data.brandFilter.id")).containsExactly(testBrand.id!!)
    }

    @Test
    @Disabled("PostGIS 함수 미지원(H2)으로 테스트 불가. 로직은 GetFilterUseCaseTest 에서 검증")
    @DisplayName("같은 브랜드의 포토부스가 여러 개여도 브랜드는 중복 없이 1건만 반환한다")
    fun givenMultipleBoothsOfSameBrand_whenGetFilter_thenReturnsBrandOnce() {
        // Given: 같은 브랜드 포토부스 2개를 영역 내에 생성
        createPhotoBoothLocation(
            brandId = testBrand.id!!,
            name = "포토이즘 강남점",
            address = "서울 강남구 강남대로 396",
            longitude = 127.027456,
            latitude = 37.497946,
        )
        createPhotoBoothLocation(
            brandId = testBrand.id!!,
            name = "포토이즘 역삼점",
            address = "서울 강남구 역삼로 123",
            longitude = 127.028123,
            latitude = 37.499123,
        )

        val request = filterGroupOf(gangnamPolygonCoordinates)

        // When
        val response = RestAssured.given()
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer $accessToken")
            .body(request)
            .`when`()
            .post("/api/photo-booths/polygon/filter")
            .then()
            .extract()

        // Then
        assertThat(response.statusCode()).isEqualTo(HttpStatus.OK.value())
        assertThat(response.jsonPath().getList<Long>("data.brandFilter.id")).containsExactly(testBrand.id!!)
    }

    @Test
    @Disabled("PostGIS 함수 미지원(H2)으로 테스트 불가. 로직은 GetFilterUseCaseTest 에서 검증")
    @DisplayName("사용자가 저장한 브랜드 정렬 순서대로 반환한다")
    fun givenUserBrandOrder_whenGetFilter_thenReturnsInSavedOrder() {
        // Given: 영역 내에 두 브랜드의 포토부스가 있고, 사용자는 역순으로 정렬을 저장
        val anotherBrand = createBrand("인생네컷", "LIFEFOURCUTS")

        createPhotoBoothLocation(
            brandId = testBrand.id!!,
            name = "포토이즘 강남점",
            address = "서울 강남구 강남대로 396",
            longitude = 127.027456,
            latitude = 37.497946,
        )
        createPhotoBoothLocation(
            brandId = anotherBrand.id!!,
            name = "인생네컷 강남점",
            address = "서울 강남구 강남대로 400",
            longitude = 127.027500,
            latitude = 37.498000,
        )

        RestAssured.given()
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer $accessToken")
            .body(MapRequest.UpdateBrandOrder(listOf(anotherBrand.id!!, testBrand.id!!)))
            .`when`()
            .put("/api/photo-booths/brand/order")
            .then()
            .statusCode(HttpStatus.OK.value())

        val request = filterGroupOf(gangnamPolygonCoordinates)

        // When
        val response = RestAssured.given()
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer $accessToken")
            .body(request)
            .`when`()
            .post("/api/photo-booths/polygon/filter")
            .then()
            .extract()

        // Then: 저장한 순서대로 반환
        assertThat(response.statusCode()).isEqualTo(HttpStatus.OK.value())
        assertThat(response.jsonPath().getList<Long>("data.brandFilter.id"))
            .containsExactly(anotherBrand.id!!, testBrand.id!!)
    }

    @Test
    @Disabled("PostGIS 함수 미지원(H2)으로 테스트 불가. 로직은 GetFilterUseCaseTest 에서 검증")
    @DisplayName("brandIds 필터와 영역의 교집합만 반환한다")
    fun givenBrandFilter_whenGetFilter_thenReturnsIntersection() {
        // Given: 영역 내에 두 브랜드가 있지만 필터는 testBrand 만 지정
        val anotherBrand = createBrand("인생네컷", "LIFEFOURCUTS")

        createPhotoBoothLocation(
            brandId = testBrand.id!!,
            name = "포토이즘 강남점",
            address = "서울 강남구 강남대로 396",
            longitude = 127.027456,
            latitude = 37.497946,
        )
        createPhotoBoothLocation(
            brandId = anotherBrand.id!!,
            name = "인생네컷 강남점",
            address = "서울 강남구 강남대로 400",
            longitude = 127.027500,
            latitude = 37.498000,
        )

        val request = filterGroupOf(gangnamPolygonCoordinates, brandIds = listOf(testBrand.id!!))

        // When
        val response = RestAssured.given()
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer $accessToken")
            .body(request)
            .`when`()
            .post("/api/photo-booths/polygon/filter")
            .then()
            .extract()

        // Then
        assertThat(response.statusCode()).isEqualTo(HttpStatus.OK.value())
        assertThat(response.jsonPath().getList<Long>("data.brandFilter.id")).containsExactly(testBrand.id!!)
    }
}
