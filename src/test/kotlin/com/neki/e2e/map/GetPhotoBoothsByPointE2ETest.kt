package com.neki.e2e.map

import com.neki.common.api.dto.BaseResponse
import com.neki.common.code.ResultCode
import com.neki.map.api.dto.GetPointLocationRequest
import com.neki.map.domain.entity.Brand
import com.neki.user.domain.entity.User
import io.restassured.RestAssured
import io.restassured.http.ContentType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.http.HttpStatus
import org.springframework.test.context.ActiveProfiles

/**
 * fileName       : GetPhotoBoothsByPointE2ETest
 * author         : darren
 * date           : 2026. 1. 21.
 * description    : 반경 내 포토부스 조회 E2E 테스트
 */
@Disabled("Native Query로 인한 테스트 불가")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class GetPhotoBoothsByPointE2ETest : MapE2ETestBase() {

    @LocalServerPort
    private var port: Int = 0

    private lateinit var accessToken: String
    private lateinit var testUser: User
    private lateinit var testBrand: Brand

    // 강남역 좌표
    private val gangnamLongitude = 127.027619
    private val gangnamLatitude = 37.497942

    @BeforeEach
    fun setUp() {
        RestAssured.port = port
        RestAssured.baseURI = "http://localhost"

        val (user, token) = createTestUserAndToken()
        testUser = user
        accessToken = token

        testBrand = createBrand("포토이즘", "PHOTOISM")
    }

    @Test
    @DisplayName("반경 내에 포토부스가 없을 때 빈 목록을 반환한다")
    fun givenNoPhotoBooths_whenGetByPoint_thenReturnsEmptyList() {
        // Given: 포토부스가 없는 상태
        val request = GetPointLocationRequest(
            longitude = gangnamLongitude,
            latitude = gangnamLatitude,
            radiusInMeters = 1000,
            brandIds = null,
        )

        // When: 반경 내 포토부스 조회 API 호출
        val response = RestAssured.given()
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer $accessToken")
            .body(request)
            .`when`()
            .post("/api/photo-booths/point")
            .then()
            .extract()

        // Then: 성공 응답 검증
        assertThat(response.statusCode()).isEqualTo(HttpStatus.OK.value())

        val baseResponse = response.`as`(BaseResponse::class.java)
        assertThat(baseResponse.resultCode).isEqualTo(ResultCode.SUCCESS.code)
    }

    @Test
    @DisplayName("반경 내에 포토부스가 있을 때 거리순으로 정렬하여 반환한다")
    fun givenPhotoBoothsInRadius_whenGetByPoint_thenReturnsPhotoBoothsOrderedByDistance() {
        // Given: 강남역 근처에 포토부스 생성
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

        val request = GetPointLocationRequest(
            longitude = gangnamLongitude,
            latitude = gangnamLatitude,
            radiusInMeters = 1000,
            brandIds = null,
        )

        // When: 반경 내 포토부스 조회 API 호출
        val response = RestAssured.given()
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer $accessToken")
            .body(request)
            .`when`()
            .post("/api/photo-booths/point")
            .then()
            .extract()

        // Then: 성공 응답 검증
        assertThat(response.statusCode()).isEqualTo(HttpStatus.OK.value())

        val baseResponse = response.`as`(BaseResponse::class.java)
        assertThat(baseResponse.resultCode).isEqualTo(ResultCode.SUCCESS.code)
    }

    @Test
    @DisplayName("특정 브랜드 ID로 필터링하여 포토부스를 조회한다")
    fun givenBrandFilter_whenGetByPoint_thenReturnsFilteredPhotoBooths() {
        // Given: 여러 브랜드의 포토부스 생성
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

        val request = GetPointLocationRequest(
            longitude = gangnamLongitude,
            latitude = gangnamLatitude,
            radiusInMeters = 1000,
            brandIds = listOf(testBrand.id!!),
        )

        // When: 특정 브랜드로 필터링하여 조회
        val response = RestAssured.given()
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer $accessToken")
            .body(request)
            .`when`()
            .post("/api/photo-booths/point")
            .then()
            .extract()

        // Then: 성공 응답 검증
        assertThat(response.statusCode()).isEqualTo(HttpStatus.OK.value())

        val baseResponse = response.`as`(BaseResponse::class.java)
        assertThat(baseResponse.resultCode).isEqualTo(ResultCode.SUCCESS.code)
    }

    @Test
    @DisplayName("반경 외부의 포토부스는 조회되지 않는다")
    fun givenPhotoBoothOutsideRadius_whenGetByPoint_thenReturnsEmptyList() {
        // Given: 반경 외부에 포토부스 생성 (부산)
        createPhotoBoothLocation(
            brandId = testBrand.id!!,
            name = "포토이즘 부산점",
            address = "부산 해운대구",
            longitude = 129.160480,
            latitude = 35.163574,
        )

        val request = GetPointLocationRequest(
            longitude = gangnamLongitude,
            latitude = gangnamLatitude,
            radiusInMeters = 1000,
            brandIds = null,
        )

        // When: 반경 내 포토부스 조회 API 호출
        val response = RestAssured.given()
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer $accessToken")
            .body(request)
            .`when`()
            .post("/api/photo-booths/point")
            .then()
            .extract()

        // Then: 성공 응답 검증
        assertThat(response.statusCode()).isEqualTo(HttpStatus.OK.value())

        val baseResponse = response.`as`(BaseResponse::class.java)
        assertThat(baseResponse.resultCode).isEqualTo(ResultCode.SUCCESS.code)
    }

    @Test
    @DisplayName("다양한 반경으로 포토부스를 조회할 수 있다")
    fun givenDifferentRadius_whenGetByPoint_thenReturnsPhotoBoothsWithinRadius() {
        // Given: 강남역에서 다양한 거리에 포토부스 생성
        createPhotoBoothLocation(
            brandId = testBrand.id!!,
            name = "포토이즘 강남점",
            address = "서울 강남구 강남대로 396",
            longitude = 127.027456,
            latitude = 37.497946,
        )

        val request = GetPointLocationRequest(
            longitude = gangnamLongitude,
            latitude = gangnamLatitude,
            radiusInMeters = 500,
            brandIds = null,
        )

        // When: 500m 반경으로 조회
        val response = RestAssured.given()
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer $accessToken")
            .body(request)
            .`when`()
            .post("/api/photo-booths/point")
            .then()
            .extract()

        // Then: 성공 응답 검증
        assertThat(response.statusCode()).isEqualTo(HttpStatus.OK.value())

        val baseResponse = response.`as`(BaseResponse::class.java)
        assertThat(baseResponse.resultCode).isEqualTo(ResultCode.SUCCESS.code)
    }
}
