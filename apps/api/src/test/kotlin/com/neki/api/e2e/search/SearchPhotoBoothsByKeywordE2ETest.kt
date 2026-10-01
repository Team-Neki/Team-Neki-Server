package com.neki.api.e2e.search

import com.neki.api.e2e.map.MapE2ETestBase
import com.neki.core.code.ResultCode
import com.neki.domain.map.models.Brand
import io.restassured.RestAssured
import org.hamcrest.Matchers.empty
import org.hamcrest.Matchers.equalTo
import org.hamcrest.Matchers.hasItem
import org.hamcrest.Matchers.not
import org.hamcrest.Matchers.nullValue
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.http.HttpStatus
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.context.ActiveProfiles
import java.time.LocalDateTime

/**
 * fileName       : SearchPhotoBoothsByKeywordE2ETest
 * author         : darren
 * date           : 2026. 10. 1.
 * description    : GET /api/search/completion/photo-booths E2E 테스트.
 *                  가까운 순 정렬은 PostgreSQL 전용 함수라 H2 에서는 검증하지 않는다 (위치를 줘도 오류 없이 동작하는 것만 확인)
 */
@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class SearchPhotoBoothsByKeywordE2ETest : MapE2ETestBase() {

    @LocalServerPort
    private var port: Int = 0

    @Autowired
    private lateinit var jdbcTemplate: JdbcTemplate

    private lateinit var accessToken: String

    @BeforeEach
    fun setUp() {
        RestAssured.port = port
        RestAssured.baseURI = "http://localhost"

        val (_, token) = createTestUserAndToken()
        accessToken = token

        val photoism: Brand = createBrand("포토이즘", "PHOTOISM")
        val lifeFourCut: Brand = createBrand("인생네컷", "LIFEFOURCUTS")
        val planB: Brand = createBrand("플랜비 스튜디오", "PLANB_STUDIO")
        val photoSignature: Brand = createBrand("포토시그니처", "PHOTOSIGNATURE")
        val deleted: Brand = brandRepository.save(
            Brand(name = "삭제브랜드", code = "DELETED", deletedAt = LocalDateTime.now()),
        )

        createPhotoBoothLocation(photoism.id!!, "강남점", "서울 강남구", 127.0276, 37.4979)
        createPhotoBoothLocation(photoism.id!!, "강남역2호점", "서울 강남구", 127.0280, 37.4985)
        createPhotoBoothLocation(photoism.id!!, "홍대점", "서울 마포구", 126.9236, 37.5563)
        createPhotoBoothLocation(lifeFourCut.id!!, "서울NC송파점", "서울 송파구", 127.1059, 37.5133)
        createPhotoBoothLocation(lifeFourCut.id!!, "강남구청점", "서울 강남구", 127.0412, 37.5172)
        createPhotoBoothLocation(planB.id!!, "강남점", "서울 강남구", 127.0290, 37.4990)
        // 수집한 지점명에는 브랜드명이 붙어 있는 것이 있다
        createPhotoBoothLocation(photoSignature.id!!, "포토시그니처 고현점", "경남 거제시", 128.6213, 34.8806)
        createPhotoBoothLocation(deleted.id!!, "강남삭제점", "서울 강남구", 127.0300, 37.5000)
        // 지점명이 비었거나 브랜드명과 같은 부스는 검색에서 빠진다
        createPhotoBoothLocation(photoism.id!!, "", "서울 중구", 126.9780, 37.5665)
        createPhotoBoothLocation(photoism.id!!, "  ", "서울 중구", 126.9781, 37.5666)
        createPhotoBoothLocation(photoism.id!!, "포토 이즘", "서울 중구", 126.9782, 37.5667)
    }

    @AfterEach
    override fun tearDown() {
        super.tearDown()
        // 삭제된 브랜드는 @SQLRestriction 때문에 JPA 로 지워지지 않는다
        jdbcTemplate.update("DELETE FROM TB_BRAND")
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
        @DisplayName("지점명 접두로 찾고 브랜드명, 지점명 순으로 `브랜드명 지점명` 을 반환한다")
        fun givenBranchPrefix_whenSearch_thenReturnsOrderedByBrandAndBranch() {
            get("keyword" to "강남")
                .statusCode(HttpStatus.OK.value())
                .body("resultCode", equalTo(ResultCode.SUCCESS.code))
                .body("data.totalCount", equalTo(4))
                .body("data.hasNext", equalTo(false))
                .body("data.items[0].keyword", equalTo("인생네컷 강남구청점"))
                .body("data.items[1].keyword", equalTo("포토이즘 강남역2호점"))
                .body("data.items[2].keyword", equalTo("포토이즘 강남점"))
                .body("data.items[3].keyword", equalTo("플랜비 스튜디오 강남점"))
        }

        @Test
        @DisplayName("브랜드명 접두로 찾으면 그 브랜드의 부스가 나온다")
        fun givenBrandPrefix_whenSearch_thenReturnsBrandBooths() {
            get("keyword" to "포토이")
                .statusCode(HttpStatus.OK.value())
                .body("data.totalCount", equalTo(3))
                .body("data.items[0].keyword", equalTo("포토이즘 강남역2호점"))
                .body("data.items[1].keyword", equalTo("포토이즘 강남점"))
                .body("data.items[2].keyword", equalTo("포토이즘 홍대점"))
        }

        @Test
        @DisplayName("`브랜드명 지점명` 으로 이어 적어도 찾는다")
        fun givenBrandAndBranch_whenSearch_thenReturnsMatchedBooths() {
            get("keyword" to "포토이즘 강남")
                .statusCode(HttpStatus.OK.value())
                .body("data.totalCount", equalTo(2))
                .body("data.items[0].keyword", equalTo("포토이즘 강남역2호점"))
                .body("data.items[1].keyword", equalTo("포토이즘 강남점"))

            get("keyword" to "포토이즘   강남점")
                .statusCode(HttpStatus.OK.value())
                .body("data.totalCount", equalTo(1))
                .body("data.items[0].keyword", equalTo("포토이즘 강남점"))
        }

        @Test
        @DisplayName("브랜드명에 공백이 있어도 찾는다")
        fun givenBrandNameWithSpace_whenSearch_thenReturnsBooths() {
            get("keyword" to "플랜비 스")
                .statusCode(HttpStatus.OK.value())
                .body("data.totalCount", equalTo(1))
                .body("data.items[0].keyword", equalTo("플랜비 스튜디오 강남점"))
        }

        @Test
        @DisplayName("대소문자를 구분하지 않는다")
        fun givenLowerCaseKeyword_whenSearch_thenIgnoresCase() {
            get("keyword" to "서울nc")
                .statusCode(HttpStatus.OK.value())
                .body("data.totalCount", equalTo(1))
                .body("data.items[0].keyword", equalTo("인생네컷 서울NC송파점"))
        }

        @Test
        @DisplayName("지점명에 브랜드명이 붙어 있으면 떼고 한 번만 붙인다")
        fun givenBranchNameWithBrandPrefix_whenSearch_thenDoesNotRepeatBrand() {
            get("keyword" to "포토시그니처")
                .statusCode(HttpStatus.OK.value())
                .body("data.totalCount", equalTo(1))
                .body("data.items[0].keyword", equalTo("포토시그니처 고현점"))
        }

        @Test
        @DisplayName("삭제된 브랜드의 부스는 나오지 않는다")
        fun givenDeletedBrand_whenSearch_thenExcludesBooths() {
            get("keyword" to "삭제")
                .statusCode(HttpStatus.OK.value())
                .body("data.items", empty<Any>())
                .body("data.totalCount", equalTo(0))
        }

        @Test
        @DisplayName("1자 검색은 빈 결과다")
        fun givenSingleCharKeyword_whenSearch_thenReturnsEmptyList() {
            get("keyword" to "강")
                .statusCode(HttpStatus.OK.value())
                .body("data.items", empty<Any>())
                .body("data.totalCount", equalTo(0))
                .body("data.hasNext", equalTo(false))
        }

        @Test
        @DisplayName("LIKE 와일드카드는 글자 그대로 찾는다")
        fun givenWildcardKeyword_whenSearch_thenMatchesLiterally() {
            get("keyword" to "강_")
                .statusCode(HttpStatus.OK.value())
                .body("data.items", empty<Any>())
                .body("data.totalCount", equalTo(0))
        }

        @Test
        @DisplayName("위치를 주면 각 부스까지의 거리를 km 로, 소수 둘째 자리에서 반올림해 내려준다")
        fun givenUserLocation_whenSearch_thenReturnsDistanceKm() {
            // 가까운 순 정렬은 PostgreSQL 전용이라 순서 대신 항목별 거리만 본다
            get("keyword" to "강남", "latitude" to 37.4979, "longitude" to 127.0276)
                .statusCode(HttpStatus.OK.value())
                .body("data.totalCount", equalTo(4))
                .body("data.items.find { it.keyword == '포토이즘 강남점' }.distanceKm", equalTo(0.0f))
                .body("data.items.find { it.keyword == '포토이즘 강남역2호점' }.distanceKm", equalTo(0.1f))
                .body("data.items.find { it.keyword == '플랜비 스튜디오 강남점' }.distanceKm", equalTo(0.2f))
                .body("data.items.find { it.keyword == '인생네컷 강남구청점' }.distanceKm", equalTo(2.5f))
        }

        @Test
        @DisplayName("위치를 안 주면 거리는 null 이다")
        fun givenNoUserLocation_whenSearch_thenDistanceIsNull() {
            get("keyword" to "강남")
                .statusCode(HttpStatus.OK.value())
                .body("data.items[0].distanceKm", nullValue())
        }

        @Test
        @DisplayName("지점명이 비었거나 브랜드명과 같은 부스는 나오지 않는다")
        fun givenBoothWithoutOwnBranchName_whenSearch_thenExcludesBooth() {
            get("keyword" to "포토이")
                .statusCode(HttpStatus.OK.value())
                .body("data.totalCount", equalTo(3))
                .body("data.items.keyword", not(hasItem("포토이즘 포토 이즘")))
        }

        @Test
        @DisplayName("페이징 - 첫 페이지는 hasNext 가 true 이고 totalCount 는 전체 건수다")
        fun givenFirstPage_whenSearch_thenHasNextIsTrueAndTotalCountIsWhole() {
            get("keyword" to "강남", "page" to 0, "size" to 3)
                .statusCode(HttpStatus.OK.value())
                .body("data.items.size()", equalTo(3))
                .body("data.hasNext", equalTo(true))
                .body("data.totalCount", equalTo(4))
        }

        @Test
        @DisplayName("페이징 - 마지막 페이지는 hasNext 가 false 다")
        fun givenLastPage_whenSearch_thenHasNextIsFalse() {
            get("keyword" to "강남", "page" to 1, "size" to 3)
                .statusCode(HttpStatus.OK.value())
                .body("data.items.size()", equalTo(1))
                .body("data.hasNext", equalTo(false))
                .body("data.items[0].keyword", equalTo("플랜비 스튜디오 강남점"))
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
        @DisplayName("위도와 경도 중 하나만 줌 - D-01")
        fun givenOnlyLatitude_whenSearch_thenReturnsInvalidParameter() {
            get("keyword" to "강남", "latitude" to 37.4979)
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
