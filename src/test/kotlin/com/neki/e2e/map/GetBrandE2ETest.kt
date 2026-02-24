package com.neki.e2e.map

import com.neki.common.api.dto.BaseResponse
import com.neki.common.api.dto.ResultCode
import com.neki.user.domain.entity.User
import io.restassured.RestAssured
import io.restassured.http.ContentType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.http.HttpStatus
import org.springframework.test.context.ActiveProfiles

/**
 * fileName       : GetBrandE2ETest
 * author         : darren
 * date           : 2026. 1. 21.
 * description    : 브랜드 조회 E2E 테스트
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class GetBrandE2ETest : MapE2ETestBase() {

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
    @DisplayName("브랜드가 없을 때 빈 목록을 반환한다")
    fun givenNoBrands_whenGetBrand_thenReturnsEmptyList() {
        // Given: 브랜드가 없는 상태

        // When: 브랜드 조회 API 호출
        val response = RestAssured.given()
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer $accessToken")
            .`when`()
            .get("/api/photo-booths/brand")
            .then()
            .extract()

        // Then: 성공 응답 및 빈 목록 검증
        assertThat(response.statusCode()).isEqualTo(HttpStatus.OK.value())

        val baseResponse = response.`as`(BaseResponse::class.java)
        assertThat(baseResponse.resultCode).isEqualTo(ResultCode.SUCCESS.code)
    }

    @Test
    @DisplayName("브랜드가 있을 때 모든 브랜드 목록을 반환한다")
    fun givenExistingBrands_whenGetBrand_thenReturnsAllBrands() {
        // Given: 복수의 브랜드 생성
        createBrand("포토이즘", "PHOTOISM")
        createBrand("인생네컷", "LIFEFOURCUTS")
        createBrand("포토그레이", "PHOTOGRAY")

        // When: 브랜드 조회 API 호출
        val response = RestAssured.given()
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer $accessToken")
            .`when`()
            .get("/api/photo-booths/brand")
            .then()
            .extract()

        // Then: 성공 응답 검증
        assertThat(response.statusCode()).isEqualTo(HttpStatus.OK.value())

        val baseResponse = response.`as`(BaseResponse::class.java)
        assertThat(baseResponse.resultCode).isEqualTo(ResultCode.SUCCESS.code)
    }
}
