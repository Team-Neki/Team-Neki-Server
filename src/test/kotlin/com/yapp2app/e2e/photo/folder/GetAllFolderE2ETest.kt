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
    @DisplayName("limit 파라미터를 전달하면 해당 개수만큼만 폴더를 반환한다")
    fun givenFoldersAndLimit_whenGetAllFolders_thenReturnsLimitedFolders() {
        // Given: 4개의 폴더 생성
        createFolders(testUser.id!!)

        // When: limit=3으로 폴더 목록 조회 API 호출
        val response = RestAssured.given()
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer $accessToken")
            .queryParam("limit", 3)
            .`when`()
            .get("/api/folders")
            .then()
            .extract()

        // Then: 성공 응답 및 3개만 반환 검증
        assertThat(response.statusCode()).isEqualTo(HttpStatus.OK.value())

        val baseResponse = response.`as`(BaseResponse::class.java)
        assertThat(baseResponse.resultCode).isEqualTo(ResultCode.SUCCESS.code)

        val data = baseResponse.data as Map<*, *>
        val items = data["items"] as List<*>
        assertThat(items).hasSize(3)
    }

    @Test
    @DisplayName("limit 파라미터가 없으면 모든 폴더를 반환한다")
    fun givenFoldersAndNoLimit_whenGetAllFolders_thenReturnsAllFolders() {
        // Given: 4개의 폴더 생성
        createFolders(testUser.id!!)

        // When: limit 없이 폴더 목록 조회 API 호출
        val response = RestAssured.given()
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer $accessToken")
            .`when`()
            .get("/api/folders")
            .then()
            .extract()

        // Then: 성공 응답 및 전체 4개 반환 검증
        assertThat(response.statusCode()).isEqualTo(HttpStatus.OK.value())

        val baseResponse = response.`as`(BaseResponse::class.java)
        assertThat(baseResponse.resultCode).isEqualTo(ResultCode.SUCCESS.code)

        val data = baseResponse.data as Map<*, *>
        val items = data["items"] as List<*>
        assertThat(items).hasSize(4)
    }

    @Test
    @DisplayName("최근에 사진이 추가된 폴더가 먼저 노출된다")
    fun givenFoldersWithPhotos_whenGetAllFolders_thenReturnsSortedByLatestPhoto() {
        // Given: 폴더 A, B, C 생성
        val folderA = folderRepository.save(Folder(userId = testUser.id!!, name = "폴더A"))
        val folderB = folderRepository.save(Folder(userId = testUser.id!!, name = "폴더B"))
        val folderC = folderRepository.save(Folder(userId = testUser.id!!, name = "폴더C"))

        // C에 사진 추가
        val mediaC = createMedia(testUser.id!!)
        createPhotoImage(testUser.id!!, mediaC.id!!, folderC.id)

        // B에 사진 추가 (C보다 나중)
        val mediaB = createMedia(testUser.id!!)
        createPhotoImage(testUser.id!!, mediaB.id!!, folderB.id)

        // When: 폴더 목록 조회 API 호출
        val response = RestAssured.given()
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer $accessToken")
            .`when`()
            .get("/api/folders")
            .then()
            .extract()

        // Then: B, C, A 순서로 반환 (사진 없는 A는 맨 뒤)
        assertThat(response.statusCode()).isEqualTo(HttpStatus.OK.value())

        val baseResponse = response.`as`(BaseResponse::class.java)
        assertThat(baseResponse.resultCode).isEqualTo(ResultCode.SUCCESS.code)

        val data = baseResponse.data as Map<*, *>
        val items = data["items"] as List<*>
        assertThat(items).hasSize(3)

        val folderNames = items.map { (it as Map<*, *>)["name"] as String }
        assertThat(folderNames).containsExactly("폴더B", "폴더C", "폴더A")
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
