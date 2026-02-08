package com.yapp2app.e2e.photo.folder

import com.yapp2app.common.api.dto.ResultCode
import com.yapp2app.photo.api.dto.CreateFolderRequest
import com.yapp2app.user.domain.entity.User
import io.restassured.RestAssured
import io.restassured.http.ContentType
import org.hamcrest.CoreMatchers.equalTo
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.http.HttpStatus
import org.springframework.test.context.ActiveProfiles

/**
 * fileName       : CreateFolderE2ETest
 * author         : koo
 * date           : 2025. 12. 28. 오후 10:14
 * description    : 폴더 생성 E2E 테스트
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class CreateFolderE2ETest : FolderE2ETestBase() {

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
    @DisplayName("유효한 폴더명으로 폴더 생성 요청 시 성공 응답을 반환한다")
    fun givenValidFolderName_whenCreateFolder_thenReturnsSuccess() {
        RestAssured.given()
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer $accessToken")
            .body(CreateFolderRequest("즐겨찾기"))
            .`when`()
            .post("/api/folders")
            .then()
            .statusCode(HttpStatus.OK.value())
            .body("resultCode", equalTo(ResultCode.SUCCESS.code))
    }

    @Test
    @DisplayName("여러 폴더를 연속으로 생성할 수 있다")
    fun givenMultipleFolderRequests_whenCreateFolders_thenAllSucceed() {
        val names = listOf("친구", "가족", "회사")

        names.forEach { folderName ->
            val request = CreateFolderRequest(name = folderName)

            RestAssured.given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer $accessToken")
                .body(request)
                .`when`()
                .post("/api/folders")
                .then()
                .statusCode(HttpStatus.OK.value())
                .body("resultCode", equalTo(ResultCode.SUCCESS.code))
        }
    }

    @Test
    @DisplayName("빈 폴더명으로 폴더 생성 요청 시 400 에러를 반환한다")
    fun givenBlankFolderName_whenCreateFolder_thenReturnsBadRequest() {
        RestAssured.given()
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer $accessToken")
            .body(CreateFolderRequest(name = ""))
            .`when`()
            .post("/api/folders")
            .then()
            .statusCode(HttpStatus.BAD_REQUEST.value())
            .body("resultCode", equalTo(ResultCode.INVALID_PARAMETER.code))
    }

    @Test
    @DisplayName("폴더명이 null인 경우 400에러를 반환한다")
    fun givenNullFolderName_whenCreateFolder_thenReturnsBadRequest() {
        val requestBody = """
            {
                "name": null
            }
        """.trimIndent()

        RestAssured.given()
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer $accessToken")
            .body(requestBody)
            .`when`()
            .post("/api/folders")
            .then()
            .statusCode(HttpStatus.BAD_REQUEST.value())
            .body("resultCode", equalTo(ResultCode.INVALID_PARAMETER.code))
    }

    @Test
    @DisplayName("폴더명이 이미 존재하는 경우 Conflict 에러를 반환한다")
    fun givenDuplicateFolderName_whenCreateFolder_thenThrowConflict() {
        val name = "즐겨찾기"

        RestAssured.given()
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer $accessToken")
            .body(CreateFolderRequest(name))
            .`when`()
            .post("/api/folders")
            .then()
            .statusCode(HttpStatus.OK.value())
            .body("resultCode", equalTo(ResultCode.SUCCESS.code))

        // 동일한 이름으로 재요청
        RestAssured.given()
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer $accessToken")
            .body(CreateFolderRequest(name))
            .`when`()
            .post("/api/folders")
            .then()
            .statusCode(HttpStatus.BAD_REQUEST.value())
            .body("resultCode", equalTo(ResultCode.CONFLICT_FOLDER.code))
    }
}
