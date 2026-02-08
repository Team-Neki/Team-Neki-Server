package com.yapp2app.e2e.photo.folder

import com.yapp2app.common.api.dto.BaseResponse
import com.yapp2app.common.api.dto.ResultCode
import com.yapp2app.photo.domain.entity.Folder
import com.yapp2app.user.domain.entity.User
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
 * fileName       : GetAllFolderE2ETest
 * author         : koo
 * date           : 2025. 12. 28. 오후 11:16
 * description    : 폴더 목록 조회 E2E 테스트
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class GetAllFolderE2ETest : FolderE2ETestBase() {

    @LocalServerPort
    private var port: Int = 0

    private lateinit var accessToken: String
    private lateinit var testUser: User

    @BeforeEach
    fun setUp() {
        RestAssured.port = port
        RestAssured.baseURI = "http://localhost"

        // 테스트용 사용자 생성 및 토큰 발급
        val (user, token) = createTestUserAndToken()
        testUser = user
        accessToken = token
    }

    private fun createFolders(userId: Long): List<Folder> = folderRepository.saveAll(
        listOf(
            Folder(userId = userId, name = "폴더1"),
            Folder(userId = userId, name = "폴더2"),
            Folder(userId = userId, name = "폴더3"),
            Folder(userId = userId, name = "폴더4"),
        ),
    )

    private fun createSingleFolder(userId: Long): Folder =
        folderRepository.save(Folder(userId = userId, name = "단일 폴더"))

    @Test
    @DisplayName("폴더가 없을 때 빈 목록을 반환한다")
    fun givenNoFolders_whenGetAllFolders_thenReturnsEmptyList() {
        // Given: 폴더가 없는 상태

        // When: 폴더 목록 조회 API 호출
        val response = RestAssured.given()
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer $accessToken")
            .`when`()
            .get("/api/folders")
            .then()
            .extract()

        // Then: 성공 응답 및 빈 목록 검증
        assertThat(response.statusCode()).isEqualTo(HttpStatus.OK.value())

        val baseResponse = response.`as`(BaseResponse::class.java)
        assertThat(baseResponse.resultCode).isEqualTo(ResultCode.SUCCESS.code)
    }

    @Test
    @DisplayName("폴더가 있을 때 모든 폴더 목록을 반환한다")
    fun givenExistingFolders_whenGetAllFolders_thenReturnsAllFolders() {
        // Given: 복수의 폴더 생성
        createFolders(testUser.id!!)

        // When: 폴더 목록 조회 API 호출
        val response = RestAssured.given()
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer $accessToken")
            .`when`()
            .get("/api/folders")
            .then()
            .extract()

        // Then: 성공 응답 및 생성된 폴더 개수 검증
        assertThat(response.statusCode()).isEqualTo(HttpStatus.OK.value())

        val baseResponse = response.`as`(BaseResponse::class.java)
        assertThat(baseResponse.resultCode).isEqualTo(ResultCode.SUCCESS.code)
    }

    @Test
    @DisplayName("단일 폴더만 있을 때 해당 폴더를 반환한다")
    fun givenSingleFolder_whenGetAllFolders_thenReturnsSingleFolder() {
        // Given: 1개의 폴더 생성
        val folderName = "테스트 폴더"
        createSingleFolder(testUser.id!!)

        // When: 폴더 목록 조회 API 호출
        val response = RestAssured.given()
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer $accessToken")
            .`when`()
            .get("/api/folders")
            .then()
            .extract()

        // Then: 성공 응답 검증
        assertThat(response.statusCode()).isEqualTo(HttpStatus.OK.value())

        val baseResponse = response.`as`(BaseResponse::class.java)
        assertThat(baseResponse.resultCode).isEqualTo(ResultCode.SUCCESS.code)
    }
}
