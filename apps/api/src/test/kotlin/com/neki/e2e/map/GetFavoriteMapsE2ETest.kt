package com.neki.e2e.map

import com.neki.common.code.ResultCode
import com.neki.map.models.Brand
import com.neki.map.models.PhotoBoothLocation
import com.neki.user.models.User
import io.restassured.RestAssured
import org.hamcrest.Matchers.equalTo
import org.hamcrest.Matchers.hasSize
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.http.HttpStatus
import org.springframework.test.context.ActiveProfiles

/**
 * fileName       : GetFavoriteMapsE2ETest
 * author         : darren
 * date           : 2026. 6. 21.
 * description    : 즐겨찾기한 포토부스 목록 조회 E2E 테스트
 */
@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class GetFavoriteMapsE2ETest : MapE2ETestBase() {

    @LocalServerPort
    private var port: Int = 0

    private lateinit var accessToken: String
    private lateinit var testUser: User
    private lateinit var testBrand: Brand

    @BeforeEach
    fun setUp() {
        RestAssured.port = port
        RestAssured.baseURI = "http://localhost"

        val (user, token) = createTestUserAndToken()
        testUser = user
        accessToken = token

        testBrand = createBrand("포토이즘", "PHOTOISM")
    }

    private fun createLocation(name: String, longitude: Double, latitude: Double): PhotoBoothLocation =
        createPhotoBoothLocation(
            brandId = testBrand.id!!,
            name = name,
            address = "서울 강남구 $name",
            longitude = longitude,
            latitude = latitude,
        )

    @Test
    @DisplayName("즐겨찾기한 포토부스가 없으면 빈 목록을 반환한다")
    fun givenNoFavorites_whenGetFavoriteMaps_thenReturnsEmptyList() {
        // when & then
        RestAssured.given()
            .header("Authorization", "Bearer $accessToken")
            .`when`()
            .get("/api/photo-booths/favorite")
            .then()
            .statusCode(HttpStatus.OK.value())
            .body("resultCode", equalTo(ResultCode.SUCCESS.code))
            .body("data.items", hasSize<Int>(0))
    }

    @Test
    @DisplayName("즐겨찾기한 포토부스 목록을 브랜드명과 함께 반환한다")
    fun givenFavorites_whenGetFavoriteMaps_thenReturnsFavoriteList() {
        // given
        val location = createLocation("강남점", 127.027456, 37.497946)
        favoriteMap(userId = testUser.id!!, locationId = location.id!!)

        // when & then
        RestAssured.given()
            .header("Authorization", "Bearer $accessToken")
            .`when`()
            .get("/api/photo-booths/favorite")
            .then()
            .statusCode(HttpStatus.OK.value())
            .body("resultCode", equalTo(ResultCode.SUCCESS.code))
            .body("data.items", hasSize<Int>(1))
            .body("data.items[0].id", equalTo(location.id!!.toInt()))
            .body("data.items[0].brandName", equalTo("포토이즘"))
            .body("data.items[0].branchName", equalTo("강남점"))
            .body("data.items[0].longitude", equalTo(127.027456f))
            .body("data.items[0].latitude", equalTo(37.497946f))
    }

    @Test
    @DisplayName("즐겨찾기한 포토부스만 조회된다")
    fun givenMixedLocations_whenGetFavoriteMaps_thenReturnsOnlyFavorites() {
        // given: 2개 생성, 1개만 즐겨찾기
        val favorited = createLocation("강남점", 127.027456, 37.497946)
        createLocation("역삼점", 127.028123, 37.499123)
        favoriteMap(userId = testUser.id!!, locationId = favorited.id!!)

        // when & then
        RestAssured.given()
            .header("Authorization", "Bearer $accessToken")
            .`when`()
            .get("/api/photo-booths/favorite")
            .then()
            .statusCode(HttpStatus.OK.value())
            .body("resultCode", equalTo(ResultCode.SUCCESS.code))
            .body("data.items", hasSize<Int>(1))
            .body("data.items[0].id", equalTo(favorited.id!!.toInt()))
    }

    @Test
    @DisplayName("다른 사용자의 즐겨찾기는 조회되지 않는다")
    fun givenOtherUserFavorite_whenGetFavoriteMaps_thenExcluded() {
        // given: 다른 사용자가 즐겨찾기한 포토부스
        val (otherUser, _) = createTestUserAndToken()
        val location = createLocation("강남점", 127.027456, 37.497946)
        favoriteMap(userId = otherUser.id!!, locationId = location.id!!)

        // when & then: 본인 즐겨찾기는 없음
        RestAssured.given()
            .header("Authorization", "Bearer $accessToken")
            .`when`()
            .get("/api/photo-booths/favorite")
            .then()
            .statusCode(HttpStatus.OK.value())
            .body("resultCode", equalTo(ResultCode.SUCCESS.code))
            .body("data.items", hasSize<Int>(0))
    }

    @Test
    @DisplayName("최근 즐겨찾기한 순서대로 반환한다")
    fun givenFavoritesInOrder_whenGetFavoriteMaps_thenReturnsInRecentFavoritedOrder() {
        // given: location A, B, C 생성 후 B → A → C 순으로 즐겨찾기
        val locationA = createLocation("A점", 127.027456, 37.497946)
        val locationB = createLocation("B점", 127.028123, 37.499123)
        val locationC = createLocation("C점", 127.029000, 37.500000)

        favoriteMap(userId = testUser.id!!, locationId = locationB.id!!)
        Thread.sleep(10) // 즐겨찾기 시간 차이 확보
        favoriteMap(userId = testUser.id!!, locationId = locationA.id!!)
        Thread.sleep(10)
        favoriteMap(userId = testUser.id!!, locationId = locationC.id!!)

        // when & then: 최근 즐겨찾기한 순서(C, A, B)대로 반환
        RestAssured.given()
            .header("Authorization", "Bearer $accessToken")
            .`when`()
            .get("/api/photo-booths/favorite")
            .then()
            .statusCode(HttpStatus.OK.value())
            .body("resultCode", equalTo(ResultCode.SUCCESS.code))
            .body("data.items", hasSize<Int>(3))
            .body("data.items[0].id", equalTo(locationC.id!!.toInt()))
            .body("data.items[1].id", equalTo(locationA.id!!.toInt()))
            .body("data.items[2].id", equalTo(locationB.id!!.toInt()))
    }
}
