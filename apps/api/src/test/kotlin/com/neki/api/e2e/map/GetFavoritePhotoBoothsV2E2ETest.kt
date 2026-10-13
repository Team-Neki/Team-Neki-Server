package com.neki.api.e2e.map

import com.neki.core.code.ResultCode
import com.neki.domain.map.models.Brand
import com.neki.domain.map.models.PhotoBoothKey
import com.neki.domain.user.models.User
import io.restassured.RestAssured
import org.hamcrest.Matchers.contains
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
 * fileName       : GetFavoritePhotoBoothsV2E2ETest
 * author         : darren
 * date           : 2026. 10. 13.
 * description    : 즐겨찾기한 포토부스 목록 조회 V2 E2E 테스트
 */
@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class GetFavoritePhotoBoothsV2E2ETest : MapE2ETestBase() {

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

    private fun getFavorites() = RestAssured.given()
        .header("Authorization", "Bearer $accessToken")
        .`when`()
        .get("/api/v2/photo-booths/favorite")
        .then()
        .statusCode(HttpStatus.OK.value())
        .body("resultCode", equalTo(ResultCode.SUCCESS.code))

    @Test
    @DisplayName("즐겨찾기한 포토부스가 없으면 빈 목록을 반환한다")
    fun givenNoFavorites_whenGet_thenReturnsEmptyList() {
        getFavorites().body("data.items", hasSize<Int>(0))
    }

    @Test
    @DisplayName("즐겨찾기한 포토부스를 포토부스 id, 브랜드명과 함께 반환한다")
    fun givenFavorite_whenGet_thenReturnsPhotoBooth() {
        // given
        val key: PhotoBoothKey = createEnrichedPhotoBooth(testBrand, "1768232303", "포토이즘 강남점", 127.027456, 37.497946)
        favoritePhotoBooth(testUser.id!!, key)

        // when & then
        getFavorites()
            .body("data.items", hasSize<Int>(1))
            .body("data.items[0].id", equalTo("source:PHOTOISM:1768232303"))
            .body("data.items[0].brandName", equalTo("포토이즘"))
            .body("data.items[0].branchName", equalTo("포토이즘 강남점"))
            .body("data.items[0].address", equalTo("서울 강남구 포토이즘 강남점"))
            .body("data.items[0].longitude", equalTo(127.027456f))
            .body("data.items[0].latitude", equalTo(37.497946f))
    }

    @Test
    @DisplayName("V1 에서 한 즐겨찾기(지점 마스터 id)도 원천 키로 바꿔 함께 반환한다")
    fun givenV1Favorite_whenGet_thenIncluded() {
        // given
        val key: PhotoBoothKey = createEnrichedPhotoBooth(testBrand, "1", "포토이즘 강남점")
        val location = createSourcedPhotoBoothLocation(testBrand, key, "포토이즘 강남점")
        favoriteMap(testUser.id!!, location.id!!)

        // when & then
        getFavorites()
            .body("data.items", hasSize<Int>(1))
            .body("data.items[0].id", equalTo(key.toBoothId()))
    }

    @Test
    @DisplayName("같은 부스를 V1 과 V2 에서 모두 즐겨찾기했으면 한 번만 반환한다")
    fun givenSameBoothFavoritedTwice_whenGet_thenReturnedOnce() {
        // given
        val key: PhotoBoothKey = createEnrichedPhotoBooth(testBrand, "1", "포토이즘 강남점")
        val location = createSourcedPhotoBoothLocation(testBrand, key, "포토이즘 강남점")
        favoriteMap(testUser.id!!, location.id!!)
        favoritePhotoBooth(testUser.id!!, key)

        // when & then
        getFavorites().body("data.items", hasSize<Int>(1))
    }

    @Test
    @DisplayName("최근 즐겨찾기한 순서대로 반환한다 (V1, V2 즐겨찾기를 섞어서)")
    fun givenFavoritesInOrder_whenGet_thenReturnsInRecentFavoritedOrder() {
        // given: B(V2) → A(V1) → C(V2) 순으로 즐겨찾기
        val keyA: PhotoBoothKey = createEnrichedPhotoBooth(testBrand, "a", "포토이즘 A점")
        val keyB: PhotoBoothKey = createEnrichedPhotoBooth(testBrand, "b", "포토이즘 B점")
        val keyC: PhotoBoothKey = createEnrichedPhotoBooth(testBrand, "c", "포토이즘 C점")
        val locationA = createSourcedPhotoBoothLocation(testBrand, keyA, "포토이즘 A점")

        favoritePhotoBooth(testUser.id!!, keyB)
        Thread.sleep(10) // 즐겨찾기 시간 차이 확보
        favoriteMap(testUser.id!!, locationA.id!!)
        Thread.sleep(10)
        favoritePhotoBooth(testUser.id!!, keyC)

        // when & then: C, A, B
        getFavorites()
            .body("data.items.id", contains(keyC.toBoothId(), keyA.toBoothId(), keyB.toBoothId()))
    }

    @Test
    @DisplayName("지금 수집 데이터에 없는 부스는 나오지 않는다")
    fun givenFavoriteOfClosedBooth_whenGet_thenExcluded() {
        // given: 즐겨찾기한 뒤 enriched 에서 빠진 부스
        favoritePhotoBooth(testUser.id!!, PhotoBoothKey(platform = "PHOTOISM", idx = "closed"))

        // when & then
        getFavorites().body("data.items", hasSize<Int>(0))
    }

    @Test
    @DisplayName("다른 사용자의 즐겨찾기는 조회되지 않는다")
    fun givenOtherUserFavorite_whenGet_thenExcluded() {
        // given
        val (otherUser, _) = createTestUserAndToken()
        val key: PhotoBoothKey = createEnrichedPhotoBooth(testBrand, "1", "포토이즘 강남점")
        favoritePhotoBooth(otherUser.id!!, key)

        // when & then
        getFavorites().body("data.items", hasSize<Int>(0))
    }
}
