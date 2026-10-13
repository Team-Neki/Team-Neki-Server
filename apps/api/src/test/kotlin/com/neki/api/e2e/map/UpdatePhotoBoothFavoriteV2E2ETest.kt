package com.neki.api.e2e.map

import com.neki.api.map.api.dto.MapRequest
import com.neki.core.code.ResultCode
import com.neki.domain.map.models.Brand
import com.neki.domain.map.models.FavoriteMapId
import com.neki.domain.map.models.FavoritePhotoBoothId
import com.neki.domain.map.models.PhotoBoothKey
import com.neki.domain.search.models.PhotoBoothEnriched
import com.neki.domain.search.models.PhotoBoothEnrichedId
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
import org.springframework.http.HttpStatus
import org.springframework.test.context.ActiveProfiles
import java.time.LocalDate

/**
 * fileName       : UpdatePhotoBoothFavoriteV2E2ETest
 * author         : darren
 * date           : 2026. 10. 13.
 * description    : 포토부스 즐겨찾기 V2 E2E 테스트
 */
@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class UpdatePhotoBoothFavoriteV2E2ETest : MapE2ETestBase() {

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

        testBrand = createBrand("브룸스튜디오", "BROOM_STUDIO")
    }

    private fun patchFavorite(photoBoothId: String, favorite: Boolean?) = RestAssured.given()
        .contentType(ContentType.JSON)
        .header("Authorization", "Bearer $accessToken")
        .body(MapRequest.UpdateMapFavorite(favorite))
        .`when`()
        .patch("/api/v2/photo-booths/$photoBoothId/favorite")
        .then()

    private fun favoriteId(key: PhotoBoothKey) = FavoritePhotoBoothId(testUser.id!!, key.platform, key.idx)

    @Test
    @DisplayName("포토부스 id 로 즐겨찾기하면 새 테이블에 키로 저장된다")
    fun givenPhotoBooth_whenFavorite_thenSavedByKey() {
        // given
        val key = createEnrichedPhotoBooth(testBrand, "1768232303", "브룸스튜디오 강남점")

        // when & then
        patchFavorite("source:BROOM_STUDIO:1768232303", true)
            .statusCode(HttpStatus.OK.value())
            .body("resultCode", equalTo(ResultCode.SUCCESS.code))

        assertThat(favoritePhotoBoothRepository.existsById(favoriteId(key))).isTrue()
    }

    @Test
    @DisplayName("여러 번 즐겨찾기를 등록해도 성공")
    fun givenPhotoBooth_whenFavoriteTwice_thenSuccess() {
        // given
        val key = createEnrichedPhotoBooth(testBrand, "1", "브룸스튜디오 강남점")

        // when & then
        repeat(2) {
            patchFavorite(key.toBoothId(), true)
                .statusCode(HttpStatus.OK.value())
                .body("resultCode", equalTo(ResultCode.SUCCESS.code))
        }

        assertThat(favoritePhotoBoothRepository.count()).isEqualTo(1)
    }

    @Test
    @DisplayName("즐겨찾기를 해제하면 V1 에서 한 같은 부스의 즐겨찾기도 함께 지워진다")
    fun givenFavoritesFromV1AndV2_whenUnmark_thenBothRemoved() {
        // given: 같은 부스를 V2(새 테이블)와 V1(지점 마스터 id, 기존 테이블)에서 즐겨찾기
        val key = createEnrichedPhotoBooth(testBrand, "1", "브룸스튜디오 강남점")
        val location = createSourcedPhotoBoothLocation(testBrand, key, "브룸스튜디오 강남점")
        favoritePhotoBooth(testUser.id!!, key)
        favoriteMap(testUser.id!!, location.id!!)

        // when & then
        patchFavorite(key.toBoothId(), false)
            .statusCode(HttpStatus.OK.value())
            .body("resultCode", equalTo(ResultCode.SUCCESS.code))

        assertThat(favoritePhotoBoothRepository.existsById(favoriteId(key))).isFalse()
        assertThat(favoriteMapRepository.existsById(FavoriteMapId(testUser.id!!, location.id!!))).isFalse()
    }

    @Test
    @DisplayName("다른 사용자의 즐겨찾기는 해제되지 않는다")
    fun givenOtherUserFavorite_whenUnmark_thenOtherUserFavoriteRemains() {
        // given
        val (otherUser, _) = createTestUserAndToken()
        val key = createEnrichedPhotoBooth(testBrand, "1", "브룸스튜디오 강남점")
        val location = createSourcedPhotoBoothLocation(testBrand, key, "브룸스튜디오 강남점")
        favoriteMap(otherUser.id!!, location.id!!)

        // when
        patchFavorite(key.toBoothId(), false).statusCode(HttpStatus.OK.value())

        // then
        assertThat(favoriteMapRepository.existsById(FavoriteMapId(otherUser.id!!, location.id!!))).isTrue()
    }

    @Test
    @DisplayName("지금 수집 데이터에 없는 부스를 즐겨찾기하면 NOT_FOUND 코드를 반환한다")
    fun givenNonExistentPhotoBooth_whenFavorite_thenReturnsNotFound() {
        patchFavorite("source:BROOM_STUDIO:99999", true)
            .statusCode(HttpStatus.BAD_REQUEST.value())
            .body("resultCode", equalTo(ResultCode.NOT_FOUND.code))
    }

    @Test
    @DisplayName("지금 수집 데이터에서 빠진 부스도 즐겨찾기를 해제할 수 있다")
    fun givenFavoriteOfRemovedBooth_whenUnmark_thenRemoved() {
        // given: 즐겨찾기한 뒤 enriched 에서 빠진 부스
        val key = PhotoBoothKey(platform = testBrand.code, idx = "closed")
        favoritePhotoBooth(testUser.id!!, key)

        // when & then
        patchFavorite(key.toBoothId(), false)
            .statusCode(HttpStatus.OK.value())
            .body("resultCode", equalTo(ResultCode.SUCCESS.code))

        assertThat(favoritePhotoBoothRepository.existsById(favoriteId(key))).isFalse()
    }

    @Test
    @DisplayName("좌표가 없어 지도에 나오지 않는 부스는 즐겨찾기할 수 없다")
    fun givenPhotoBoothWithoutLocation_whenFavorite_thenReturnsNotFound() {
        // given
        photoBoothEnrichedRepository.save(
            PhotoBoothEnriched(
                id = PhotoBoothEnrichedId(platform = testBrand.code, idx = "nocoord"),
                name = "브룸스튜디오 좌표없음점",
                address = null,
                location = null,
                sourceDt = LocalDate.of(2026, 10, 12),
                bCode = null,
            ),
        )

        // when & then
        patchFavorite("source:BROOM_STUDIO:nocoord", true)
            .statusCode(HttpStatus.BAD_REQUEST.value())
            .body("resultCode", equalTo(ResultCode.NOT_FOUND.code))
    }

    @Test
    @DisplayName("모양이 맞지 않는 포토부스 id 면 INVALID_PARAMETER 코드를 반환한다")
    fun givenMalformedPhotoBoothId_whenFavorite_thenReturnsInvalidParameter() {
        patchFavorite("12345", true)
            .statusCode(HttpStatus.BAD_REQUEST.value())
            .body("resultCode", equalTo(ResultCode.INVALID_PARAMETER.code))
    }

    @Test
    @DisplayName("favorite 값이 없으면 검증 에러를 반환한다")
    fun givenNullFavorite_whenFavorite_thenReturnsBadRequest() {
        val key = createEnrichedPhotoBooth(testBrand, "1", "브룸스튜디오 강남점")

        patchFavorite(key.toBoothId(), null)
            .statusCode(HttpStatus.BAD_REQUEST.value())
    }

    @Test
    @DisplayName("토큰이 없으면 403")
    fun givenNoToken_whenFavorite_thenForbidden() {
        RestAssured.given()
            .contentType(ContentType.JSON)
            .body(MapRequest.UpdateMapFavorite(true))
            .`when`()
            .patch("/api/v2/photo-booths/source:BROOM_STUDIO:1/favorite")
            .then()
            .statusCode(HttpStatus.FORBIDDEN.value())
    }
}
