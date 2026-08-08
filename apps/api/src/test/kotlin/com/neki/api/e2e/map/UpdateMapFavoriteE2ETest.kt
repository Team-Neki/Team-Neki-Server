package com.neki.api.e2e.map

import com.neki.api.map.api.dto.MapRequest
import com.neki.core.code.ResultCode
import com.neki.domain.map.models.Brand
import com.neki.domain.map.models.FavoriteMapId
import com.neki.domain.user.models.User
import io.restassured.RestAssured
import io.restassured.http.ContentType
import org.assertj.core.api.Assertions.assertThat
import org.hamcrest.Matchers.equalTo
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.data.repository.findByIdOrNull
import org.springframework.http.HttpStatus
import org.springframework.test.context.ActiveProfiles

/**
 * fileName       : UpdateMapFavoriteE2ETest
 * author         : darren
 * date           : 2026. 6. 21.
 * description    : 포토부스 즐겨찾기 E2E 테스트
 */
@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class UpdateMapFavoriteE2ETest : MapE2ETestBase() {

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

    @Test
    @DisplayName("포토부스 즐겨찾기 성공")
    fun givenPhotoBooth_whenFavorite_thenReturnSuccess() {
        // given
        val location = createPhotoBoothLocation(
            brandId = testBrand.id!!,
            name = "포토이즘 강남점",
            address = "서울 강남구 강남대로 396",
            longitude = 127.027456,
            latitude = 37.497946,
        )

        // when & then
        RestAssured.given()
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer $accessToken")
            .body(MapRequest.UpdateMapFavorite(true))
            .`when`()
            .patch("/api/photo-booths/${location.id}/favorite")
            .then()
            .statusCode(HttpStatus.OK.value())
            .body("resultCode", equalTo(ResultCode.SUCCESS.code))

        val favoriteMap = favoriteMapRepository.findByIdOrNull(
            FavoriteMapId(testUser.id!!, location.id!!),
        )
        assertThat(favoriteMap).isNotNull()
    }

    @Test
    @DisplayName("여러 번 즐겨찾기를 등록해도 성공")
    fun givenPhotoBooth_whenFavoriteTwice_thenReturnSuccess() {
        // given
        val location = createPhotoBoothLocation(
            brandId = testBrand.id!!,
            name = "포토이즘 강남점",
            address = "서울 강남구 강남대로 396",
            longitude = 127.027456,
            latitude = 37.497946,
        )

        // when & then
        repeat(2) {
            RestAssured.given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer $accessToken")
                .body(MapRequest.UpdateMapFavorite(true))
                .`when`()
                .patch("/api/photo-booths/${location.id}/favorite")
                .then()
                .statusCode(HttpStatus.OK.value())
                .body("resultCode", equalTo(ResultCode.SUCCESS.code))
        }

        val favoriteMap = favoriteMapRepository.findByIdOrNull(
            FavoriteMapId(testUser.id!!, location.id!!),
        )
        assertThat(favoriteMap).isNotNull()
    }

    @Test
    @DisplayName("즐겨찾기한 포토부스를 즐겨찾기에서 삭제하면 성공")
    fun givenFavoritePhotoBooth_whenUnmarkFavorite_thenSuccess() {
        // given
        val location = createPhotoBoothLocation(
            brandId = testBrand.id!!,
            name = "포토이즘 강남점",
            address = "서울 강남구 강남대로 396",
            longitude = 127.027456,
            latitude = 37.497946,
        )
        favoriteMap(userId = testUser.id!!, locationId = location.id!!)

        // when & then
        RestAssured.given()
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer $accessToken")
            .body(MapRequest.UpdateMapFavorite(false))
            .`when`()
            .patch("/api/photo-booths/${location.id}/favorite")
            .then()
            .statusCode(HttpStatus.OK.value())
            .body("resultCode", equalTo(ResultCode.SUCCESS.code))

        val favoriteMap = favoriteMapRepository.findByIdOrNull(
            FavoriteMapId(testUser.id!!, location.id!!),
        )
        assertThat(favoriteMap).isNull()
    }

    @Test
    @DisplayName("존재하지 않는 포토부스를 즐겨찾기하면 NOT_FOUND 코드를 반환한다")
    fun givenNonExistentPhotoBooth_whenFavorite_thenReturnsNotFound() {
        // when & then
        RestAssured.given()
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer $accessToken")
            .body(MapRequest.UpdateMapFavorite(true))
            .`when`()
            .patch("/api/photo-booths/99999/favorite")
            .then()
            .statusCode(HttpStatus.BAD_REQUEST.value())
            .body("resultCode", equalTo(ResultCode.NOT_FOUND.code))
    }

    @Test
    @DisplayName("favorite 값이 없으면 검증 에러를 반환한다")
    fun givenNullFavorite_whenFavorite_thenReturnsBadRequest() {
        // given
        val location = createPhotoBoothLocation(
            brandId = testBrand.id!!,
            name = "포토이즘 강남점",
            address = "서울 강남구 강남대로 396",
            longitude = 127.027456,
            latitude = 37.497946,
        )

        // when & then
        RestAssured.given()
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer $accessToken")
            .body(MapRequest.UpdateMapFavorite(null))
            .`when`()
            .patch("/api/photo-booths/${location.id}/favorite")
            .then()
            .statusCode(HttpStatus.BAD_REQUEST.value())
    }
}
