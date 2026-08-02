package com.neki.e2e.map

import com.neki.common.code.ResultCode
import com.neki.map.api.dto.MapRequest
import com.neki.user.entity.User
import io.restassured.RestAssured
import io.restassured.http.ContentType
import org.assertj.core.api.Assertions.assertThat
import org.hamcrest.Matchers.contains
import org.hamcrest.Matchers.equalTo
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.http.HttpStatus
import org.springframework.test.context.ActiveProfiles

/**
 * fileName       : UpdateBrandOrderE2ETest
 * author         : darren
 * date           : 2026. 6. 22.
 * description    : 브랜드 정렬 순서 저장 및 조회 반영 E2E 테스트
 */
@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class UpdateBrandOrderE2ETest : MapE2ETestBase() {

    @LocalServerPort
    private var port: Int = 0

    private lateinit var accessToken: String
    private lateinit var testUser: User

    @BeforeEach
    fun setUp() {
        RestAssured.port = port
        RestAssured.baseURI = "http://localhost"

        val (user, token) = createTestUserAndToken()
        testUser = user
        accessToken = token
    }

    @Test
    @DisplayName("브랜드 정렬 순서를 저장하면 sort_order가 전달한 순서대로 저장된다")
    fun givenBrands_whenUpdateOrder_thenPersistedInOrder() {
        // given
        val photoism = createBrand("포토이즘", "PHOTOISM")
        val lifefour = createBrand("인생네컷", "LIFEFOURCUTS")
        val photogray = createBrand("포토그레이", "PHOTOGRAY")

        // when: 기본 id 순서(photoism, lifefour, photogray)와 다른 순서로 저장
        RestAssured.given()
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer $accessToken")
            .body(MapRequest.UpdateBrandOrder(listOf(photogray.id!!, photoism.id!!, lifefour.id!!)))
            .`when`()
            .put("/api/photo-booths/brand/order")
            .then()
            .statusCode(HttpStatus.OK.value())
            .body("resultCode", equalTo(ResultCode.SUCCESS.code))

        // then: sort_order 가 0,1,2 로 저장됨
        val saved = userBrandOrderRepository.findAllByIdUserId(testUser.id!!)
            .associate { it.id.brandId to it.sortOrder }
        assertThat(saved).containsExactlyInAnyOrderEntriesOf(
            mapOf(photogray.id!! to 0, photoism.id!! to 1, lifefour.id!! to 2),
        )
    }

    @Test
    @DisplayName("정렬 순서를 저장하면 브랜드 조회 API가 저장한 순서대로 반환한다")
    fun givenSavedOrder_whenGetBrand_thenReturnsInSavedOrder() {
        // given
        val photoism = createBrand("포토이즘", "PHOTOISM")
        val lifefour = createBrand("인생네컷", "LIFEFOURCUTS")
        val photogray = createBrand("포토그레이", "PHOTOGRAY")

        updateOrder(listOf(photogray.id!!, photoism.id!!, lifefour.id!!))

        // when & then: 저장한 순서대로 반환
        RestAssured.given()
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer $accessToken")
            .`when`()
            .get("/api/photo-booths/brand")
            .then()
            .statusCode(HttpStatus.OK.value())
            .body("data.code", contains("PHOTOGRAY", "PHOTOISM", "LIFEFOURCUTS"))
    }

    @Test
    @DisplayName("정렬 순서를 저장하지 않으면 브랜드 조회 API는 서버 기본 순서(id 오름차순)로 반환한다")
    fun givenNoSavedOrder_whenGetBrand_thenReturnsInDefaultOrder() {
        // given
        createBrand("포토이즘", "PHOTOISM")
        createBrand("인생네컷", "LIFEFOURCUTS")
        createBrand("포토그레이", "PHOTOGRAY")

        // when & then: 생성(=id) 순서대로 반환
        RestAssured.given()
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer $accessToken")
            .`when`()
            .get("/api/photo-booths/brand")
            .then()
            .statusCode(HttpStatus.OK.value())
            .body("data.code", contains("PHOTOISM", "LIFEFOURCUTS", "PHOTOGRAY"))
    }

    @Test
    @DisplayName("정렬 저장 이후 추가된 브랜드는 저장된 순서 뒤쪽에 id 순으로 반환된다")
    fun givenBrandAddedAfterOrdering_whenGetBrand_thenAppendedAtTheEnd() {
        // given
        val photoism = createBrand("포토이즘", "PHOTOISM")
        val lifefour = createBrand("인생네컷", "LIFEFOURCUTS")
        updateOrder(listOf(lifefour.id!!, photoism.id!!))

        // 정렬 저장 이후 새로운 브랜드 추가
        createBrand("포토그레이", "PHOTOGRAY")

        // when & then: 저장된 순서(lifefour, photoism) 뒤에 신규 브랜드가 붙음
        RestAssured.given()
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer $accessToken")
            .`when`()
            .get("/api/photo-booths/brand")
            .then()
            .statusCode(HttpStatus.OK.value())
            .body("data.code", contains("LIFEFOURCUTS", "PHOTOISM", "PHOTOGRAY"))
    }

    @Test
    @DisplayName("정렬 순서를 다시 저장하면 기존 순서가 새 순서로 대체된다")
    fun givenExistingOrder_whenUpdateAgain_thenReplaced() {
        // given
        val photoism = createBrand("포토이즘", "PHOTOISM")
        val lifefour = createBrand("인생네컷", "LIFEFOURCUTS")
        updateOrder(listOf(photoism.id!!, lifefour.id!!))

        // when: 다른 순서로 다시 저장
        updateOrder(listOf(lifefour.id!!, photoism.id!!))

        // then: 새 순서로 대체됨
        val saved = userBrandOrderRepository.findAllByIdUserId(testUser.id!!)
            .associate { it.id.brandId to it.sortOrder }
        assertThat(saved).containsExactlyInAnyOrderEntriesOf(
            mapOf(lifefour.id!! to 0, photoism.id!! to 1),
        )
    }

    @Test
    @DisplayName("존재하지 않는 브랜드 ID가 포함되면 NOT_FOUND 코드를 반환한다")
    fun givenNonExistentBrandId_whenUpdateOrder_thenReturnsNotFound() {
        // given
        val photoism = createBrand("포토이즘", "PHOTOISM")

        // when & then
        RestAssured.given()
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer $accessToken")
            .body(MapRequest.UpdateBrandOrder(listOf(photoism.id!!, 99999L)))
            .`when`()
            .put("/api/photo-booths/brand/order")
            .then()
            .statusCode(HttpStatus.BAD_REQUEST.value())
            .body("resultCode", equalTo(ResultCode.NOT_FOUND.code))
    }

    @Test
    @DisplayName("brandIds가 비어있으면 검증 에러를 반환한다")
    fun givenEmptyBrandIds_whenUpdateOrder_thenReturnsBadRequest() {
        // when & then
        RestAssured.given()
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer $accessToken")
            .body(MapRequest.UpdateBrandOrder(emptyList()))
            .`when`()
            .put("/api/photo-booths/brand/order")
            .then()
            .statusCode(HttpStatus.BAD_REQUEST.value())
    }

    private fun updateOrder(brandIds: List<Long>) {
        RestAssured.given()
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer $accessToken")
            .body(MapRequest.UpdateBrandOrder(brandIds))
            .`when`()
            .put("/api/photo-booths/brand/order")
            .then()
            .statusCode(HttpStatus.OK.value())
    }
}
