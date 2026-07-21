package com.neki.e2e.photo.folder

import com.neki.common.api.dto.BaseResponse
import com.neki.common.code.ResultCode
import com.neki.e2e.E2ETestBase
import com.neki.photo.api.dto.FolderRequest
import com.neki.photo.api.dto.FolderResponse
import com.neki.photo.domain.entity.Folder
import com.neki.photo.infra.persist.jpa.JpaFolderRepository
import com.neki.user.domain.entity.User
import io.restassured.RestAssured
import io.restassured.common.mapper.TypeRef
import io.restassured.http.ContentType
import org.hamcrest.CoreMatchers.equalTo
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.http.HttpStatus
import org.springframework.test.context.ActiveProfiles

/**
 * fileName       : UpdateFolderE2ETest
 * author         : koo
 * date           : 2025. 12. 29.
 * description    : 폴더 갱신 E2E 테스트
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class UpdateFolderE2ETest : E2ETestBase() {

    @LocalServerPort
    private var port: Int = 0

    private lateinit var accessToken: String
    private lateinit var testUser: User

    @Autowired
    private lateinit var folderRepository: JpaFolderRepository

    @BeforeEach
    fun setUp() {
        // 테스트 시작 전 데이터 정리 (자식 → 부모 순서)
        folderRepository.deleteAllInBatch()
        userRepository.deleteAllInBatch()

        RestAssured.port = port
        RestAssured.baseURI = "http://localhost"

        // Given: 테스트용 사용자 생성 및 토큰 발급
        val (user, token) = createTestUserAndToken()
        testUser = user
        accessToken = token
    }

    @Test
    @DisplayName("유효한 폴더명으로 폴더 갱신 시 성공 응답을 반환한다")
    fun givenValidFolderName_whenUpdateFolder_thenReturnsSuccess() {
        // Given: 폴더 생성
        val folder = folderRepository.save(
            Folder(userId = testUser.id!!, name = "원래 이름"),
        )

        // When & Then: 폴더명 갱신 요청
        RestAssured.given()
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer $accessToken")
            .body(FolderRequest.UpdateFolder(name = "변경된 이름"))
            .`when`()
            .patch("/api/folders/${folder.id}")
            .then()
            .statusCode(HttpStatus.OK.value())
            .body("resultCode", equalTo(ResultCode.SUCCESS.code))
    }

    @Test
    @DisplayName("빈 폴더명으로 갱신 시 400 에러를 반환한다")
    fun givenBlankFolderName_whenUpdateFolder_thenReturnsBadRequest() {
        // Given: 폴더 생성
        val folder = folderRepository.save(
            Folder(userId = testUser.id!!, name = "원래 이름"),
        )

        // When & Then: 빈 폴더명으로 갱신 요청
        RestAssured.given()
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer $accessToken")
            .body(FolderRequest.UpdateFolder(name = ""))
            .`when`()
            .patch("/api/folders/${folder.id}")
            .then()
            .statusCode(HttpStatus.BAD_REQUEST.value())
            .body("resultCode", equalTo(ResultCode.INVALID_PARAMETER.code))
    }

    @Test
    @DisplayName("폴더명이 null인 경우 400 에러를 반환한다")
    fun givenNullFolderName_whenUpdateFolder_thenReturnsBadRequest() {
        // Given: 폴더 생성
        val folder = folderRepository.save(
            Folder(userId = testUser.id!!, name = "원래 이름"),
        )

        val requestBody = """
            {
                "name": null
            }
        """.trimIndent()

        // When & Then: null 폴더명으로 갱신 요청
        RestAssured.given()
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer $accessToken")
            .body(requestBody)
            .`when`()
            .patch("/api/folders/${folder.id}")
            .then()
            .statusCode(HttpStatus.BAD_REQUEST.value())
            .body("resultCode", equalTo(ResultCode.INVALID_PARAMETER.code))
    }

    @Test
    @DisplayName("존재하지 않는 폴더 갱신 시 400 에러를 반환한다")
    fun givenNonExistentFolder_whenUpdateFolder_thenReturnsNotFound() {
        // Given: 존재하지 않는 폴더 ID
        val nonExistentFolderId = 99999L

        // When & Then: 존재하지 않는 폴더 갱신 요청
        RestAssured.given()
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer $accessToken")
            .body(FolderRequest.UpdateFolder(name = "새 이름"))
            .`when`()
            .patch("/api/folders/$nonExistentFolderId")
            .then()
            .statusCode(HttpStatus.BAD_REQUEST.value())
            .body("resultCode", equalTo(ResultCode.NOT_FOUND.code))
    }

    @Test
    @DisplayName("이미 존재하는 폴더명으로 변경 시 Conflict 에러를 반환한다")
    fun givenDuplicateFolderName_whenUpdateFolder_thenReturnsConflict() {
        // Given: 두 개의 폴더 생성
        val response = RestAssured.given()
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer $accessToken")
            .body(FolderRequest.CreateFolder("폴더1"))
            .`when`()
            .post("/api/folders")
            .then()
            .statusCode(HttpStatus.OK.value())
            .extract()
            .`as`(object : TypeRef<BaseResponse<FolderResponse.CreateFolder>>() {})

        RestAssured.given()
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer $accessToken")
            .body(FolderRequest.CreateFolder("폴더2"))
            .`when`()
            .post("/api/folders")
            .then()
            .statusCode(HttpStatus.OK.value())

        println(response)

        // When & Then: 이미 존재하는 폴더명으로 갱신 요청
        RestAssured.given()
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer $accessToken")
            .body(FolderRequest.UpdateFolder(name = "폴더2"))
            .`when`()
            .patch("/api/folders/${response.data?.folderId}")
            .then()
            .statusCode(HttpStatus.BAD_REQUEST.value())
            .body("resultCode", equalTo(ResultCode.CONFLICT_FOLDER.code))
    }

    @Test
    @DisplayName("다른 사용자의 폴더 갱신 시 400 에러를 반환한다")
    fun givenOtherUserFolder_whenUpdateFolder_thenReturnsNotFound() {
        // Given: 다른 사용자 생성 및 해당 사용자의 폴더 생성
        val (otherUser, _) = createTestUserAndToken() // 기본값으로 unique한 이메일 생성
        val otherUserFolder = folderRepository.save(
            Folder(userId = otherUser.id!!, name = "다른 사용자 폴더"),
        )

        // When & Then: 다른 사용자의 폴더 갱신 시도 (존재하지 않는 것처럼 처리)
        RestAssured.given()
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer $accessToken")
            .body(FolderRequest.UpdateFolder(name = "새 이름"))
            .`when`()
            .patch("/api/folders/${otherUserFolder.id}")
            .then()
            .statusCode(HttpStatus.BAD_REQUEST.value())
            .body("resultCode", equalTo(ResultCode.NOT_FOUND.code))
    }

    @Test
    @DisplayName("동일한 폴더명으로 갱신하면 성공한다")
    fun givenSameFolderName_whenUpdateFolder_thenReturnsSuccess() {
        // Given: 폴더 생성
        val originalName = "동일한 이름"
        val folder = folderRepository.save(
            Folder(userId = testUser.id!!, name = originalName),
        )

        // When & Then: 동일한 폴더명으로 갱신 요청
        RestAssured.given()
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer $accessToken")
            .body(FolderRequest.UpdateFolder(name = originalName))
            .`when`()
            .patch("/api/folders/${folder.id}")
            .then()
            .statusCode(HttpStatus.OK.value())
            .body("resultCode", equalTo(ResultCode.SUCCESS.code))
    }

    @Test
    @DisplayName("폴더명이 10자를 초과하면 400 에러를 반환한다")
    fun givenTooLongFolderName_whenUpdateFolder_thenReturnsBadRequest() {
        val folder = folderRepository.save(
            Folder(userId = testUser.id!!, name = "원래 이름"),
        )

        RestAssured.given()
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer $accessToken")
            .body(FolderRequest.UpdateFolder(name = "일이삼사오육칠팔구십일")) // 11자
            .`when`()
            .patch("/api/folders/${folder.id}")
            .then()
            .statusCode(HttpStatus.BAD_REQUEST.value())
            .body("resultCode", equalTo(ResultCode.INVALID_PARAMETER.code))
    }
}
