package com.yapp2app.e2e.photo.folder

import com.yapp2app.common.api.dto.BaseResponse
import com.yapp2app.common.api.dto.ResultCode
import com.yapp2app.photo.api.dto.CreateFolderRequest
import com.yapp2app.photo.api.dto.CreateFolderResponse
import com.yapp2app.photo.domain.entity.Folder
import com.yapp2app.user.domain.entity.User
import io.restassured.RestAssured
import io.restassured.common.mapper.TypeRef
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
 * fileName       : DeleteFolderE2ETest
 * author         : koo
 * date           : 2025. 12. 29.
 * description    : 단건 폴더 삭제 E2E 테스트
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class DeleteFolderE2ETest : FolderE2ETestBase() {

    @LocalServerPort
    private var port: Int = 0

    private lateinit var accessToken: String
    private lateinit var testUser: User

    @BeforeEach
    fun setUp() {
        RestAssured.port = port
        RestAssured.baseURI = "http://localhost"

        // Given: 테스트용 사용자 생성 및 토큰 발급
        val (user, token) = createTestUserAndToken()
        testUser = user
        accessToken = token
    }

    @Test
    @DisplayName("존재하는 폴더 삭제 시 성공 응답을 반환한다")
    fun givenExistingFolder_whenDeleteFolder_thenReturnsSuccess() {
        // Given: 폴더 생성
        val response = RestAssured.given()
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer $accessToken")
            .body(CreateFolderRequest("삭제할 폴더"))
            .`when`()
            .post("/api/folders")
            .then()
            .statusCode(HttpStatus.OK.value())
            .body("success", equalTo(true))
            .body("resultCode", equalTo(ResultCode.SUCCESS.code))
            .extract()
            .`as`(object : TypeRef<BaseResponse<CreateFolderResponse>>() {})

        // When & Then: 폴더 삭제 요청
        RestAssured.given()
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer $accessToken")
            .`when`()
            .delete("/api/folders/${response.data?.folderId}")
            .then()
            .statusCode(HttpStatus.OK.value())
            .body("success", equalTo(true))
            .body("resultCode", equalTo(ResultCode.SUCCESS.code))
    }

    @Test
    @DisplayName("존재하지 않는 폴더 삭제 시 400 에러를 반환한다")
    fun givenNonExistentFolder_whenDeleteFolder_thenReturnsNotFound() {
        // Given: 존재하지 않는 폴더 ID
        val nonExistentFolderId = 99999L

        // When & Then: 존재하지 않는 폴더 삭제 요청
        RestAssured.given()
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer $accessToken")
            .`when`()
            .delete("/api/folders/$nonExistentFolderId")
            .then()
            .statusCode(HttpStatus.BAD_REQUEST.value())
            .body("success", equalTo(false))
            .body("resultCode", equalTo(ResultCode.NOT_FOUND.code))
    }

    @Test
    @DisplayName("다른 사용자의 폴더 삭제 시 400 에러를 반환한다")
    fun givenOtherUserFolder_whenDeleteFolder_thenReturnsNotFound() {
        // Given: 다른 사용자 생성 및 해당 사용자의 폴더 생성
        val (otherUser, _) = createTestUserAndToken(email = "other@example.com")
        val otherUserFolder = folderRepository.save(
            Folder(userId = otherUser.id!!, name = "다른 사용자 폴더"),
        )

        // When & Then: 다른 사용자의 폴더 삭제 시도 (존재하지 않는 것처럼 처리)
        RestAssured.given()
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer $accessToken")
            .`when`()
            .delete("/api/folders/${otherUserFolder.id}")
            .then()
            .statusCode(HttpStatus.BAD_REQUEST.value())
            .body("success", equalTo(false))
            .body("resultCode", equalTo(ResultCode.NOT_FOUND.code))
    }
}
