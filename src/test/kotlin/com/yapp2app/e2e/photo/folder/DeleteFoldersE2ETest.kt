package com.yapp2app.e2e.photo.folder

import com.yapp2app.common.api.dto.ResultCode
import com.yapp2app.e2e.photo.image.PhotoImageE2ETestBase
import com.yapp2app.media.domain.entity.MediaStatus
import com.yapp2app.photo.api.dto.DeleteFoldersRequest
import com.yapp2app.photo.domain.entity.Folder
import com.yapp2app.user.domain.entity.User
import io.restassured.RestAssured
import io.restassured.http.ContentType
import org.assertj.core.api.Assertions.assertThat
import org.hamcrest.CoreMatchers.equalTo
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.http.HttpStatus
import org.springframework.test.context.ActiveProfiles

/**
 * fileName       : DeleteFoldersE2ETest
 * author         : koo
 * date           : 2025. 12. 29.
 * description    : 폴더 선택 삭제 E2E 테스트
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class DeleteFoldersE2ETest : PhotoImageE2ETestBase() {

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
    @DisplayName("여러 개의 폴더를 선택하여 삭제 시 성공 응답을 반환한다")
    fun givenMultipleFolders_whenDeleteFolders_thenReturnsSuccess() {
        // Given: 여러 폴더 생성
        val folders = folderRepository.saveAll(
            listOf(
                Folder(userId = testUser.id!!, name = "폴더1"),
                Folder(userId = testUser.id!!, name = "폴더2"),
                Folder(userId = testUser.id!!, name = "폴더3"),
            ),
        )
        val folderIds = folders.map { it.id!! }

        // When & Then: 여러 폴더 삭제 요청
        RestAssured.given()
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer $accessToken")
            .body(DeleteFoldersRequest(folderIds = folderIds))
            .`when`()
            .delete("/api/folders")
            .then()
            .statusCode(HttpStatus.OK.value())
            .body("resultCode", equalTo(ResultCode.SUCCESS.code))
    }

    @Test
    @DisplayName("단일 폴더만 선택하여 삭제해도 성공한다")
    fun givenSingleFolder_whenDeleteFolders_thenReturnsSuccess() {
        // Given: 단일 폴더 생성
        val folder = folderRepository.save(
            Folder(userId = testUser.id!!, name = "단일 폴더"),
        )

        // When & Then: 단일 폴더 삭제 요청
        RestAssured.given()
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer $accessToken")
            .body(DeleteFoldersRequest(folderIds = listOf(folder.id!!)))
            .`when`()
            .delete("/api/folders")
            .then()
            .statusCode(HttpStatus.OK.value())
            .body("resultCode", equalTo(ResultCode.SUCCESS.code))
    }

    @Test
    @DisplayName("빈 폴더 ID 리스트로 삭제 요청 시 400 에러를 반환한다")
    fun givenEmptyFolderIds_whenDeleteFolders_thenReturnsBadRequest() {
        // When & Then: 빈 리스트로 삭제 요청
        RestAssured.given()
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer $accessToken")
            .body(DeleteFoldersRequest(folderIds = emptyList()))
            .`when`()
            .delete("/api/folders")
            .then()
            .statusCode(HttpStatus.BAD_REQUEST.value())
            .body("resultCode", equalTo(ResultCode.INVALID_PARAMETER.code))
    }

    @Test
    @DisplayName("존재하지 않는 폴더 ID가 포함된 경우 400 에러를 반환한다")
    fun givenNonExistentFolderId_whenDeleteFolders_thenReturnsNotFound() {
        // Given: 실제 폴더와 존재하지 않는 폴더 ID
        val folder = folderRepository.save(
            Folder(userId = testUser.id!!, name = "실제 폴더"),
        )
        val nonExistentId = 99999L
        val folderIds = listOf(folder.id!!, nonExistentId)

        // When & Then: 존재하지 않는 폴더 ID가 포함된 삭제 요청
        RestAssured.given()
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer $accessToken")
            .body(DeleteFoldersRequest(folderIds = folderIds))
            .`when`()
            .delete("/api/folders")
            .then()
            .statusCode(HttpStatus.BAD_REQUEST.value())
            .body("resultCode", equalTo(ResultCode.NOT_FOUND.code))
    }

    @Test
    @DisplayName("다른 사용자의 폴더 ID가 포함된 경우 400 에러를 반환한다")
    fun givenOtherUserFolderId_whenDeleteFolders_thenReturnsNotFound() {
        // Given: 현재 사용자의 폴더와 다른 사용자의 폴더
        val myFolder = folderRepository.save(
            Folder(userId = testUser.id!!, name = "내 폴더"),
        )
        val (otherUser, _) = createTestUserAndToken(email = "other@example.com")
        val otherUserFolder = folderRepository.save(
            Folder(userId = otherUser.id!!, name = "다른 사용자 폴더"),
        )
        val folderIds = listOf(myFolder.id!!, otherUserFolder.id!!)

        // When & Then: 다른 사용자의 폴더 ID가 포함된 삭제 요청 (존재하지 않는 것처럼 처리)
        RestAssured.given()
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer $accessToken")
            .body(DeleteFoldersRequest(folderIds = folderIds))
            .`when`()
            .delete("/api/folders")
            .then()
            .statusCode(HttpStatus.BAD_REQUEST.value())
            .body("resultCode", equalTo(ResultCode.NOT_FOUND.code))
    }

    @Test
    @DisplayName("단일 폴더 삭제 시 존재하지 않는 폴더인 경우 400 에러를 반환한다")
    fun givenNonExistentSingleFolder_whenDeleteFolders_thenReturnsNotFound() {
        // Given: 존재하지 않는 폴더 ID
        val nonExistentFolderId = 99999L

        // When & Then: 존재하지 않는 단일 폴더 삭제 요청
        RestAssured.given()
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer $accessToken")
            .body(DeleteFoldersRequest(folderIds = listOf(nonExistentFolderId)))
            .`when`()
            .delete("/api/folders")
            .then()
            .statusCode(HttpStatus.BAD_REQUEST.value())
            .body("resultCode", equalTo(ResultCode.NOT_FOUND.code))
    }

    @Test
    @DisplayName("단일 폴더 삭제 시 다른 사용자의 폴더인 경우 400 에러를 반환한다")
    fun givenOtherUserSingleFolder_whenDeleteFolders_thenReturnsNotFound() {
        // Given: 다른 사용자 생성 및 해당 사용자의 폴더 생성
        val (otherUser, _) = createTestUserAndToken(email = "other2@example.com")
        val otherUserFolder = folderRepository.save(
            Folder(userId = otherUser.id!!, name = "다른 사용자 폴더"),
        )

        // When & Then: 다른 사용자의 단일 폴더 삭제 시도 (존재하지 않는 것처럼 처리)
        RestAssured.given()
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer $accessToken")
            .body(DeleteFoldersRequest(folderIds = listOf(otherUserFolder.id!!)))
            .`when`()
            .delete("/api/folders")
            .then()
            .statusCode(HttpStatus.BAD_REQUEST.value())
            .body("resultCode", equalTo(ResultCode.NOT_FOUND.code))
    }

    @Test
    @DisplayName("deletePhotos=true로 폴더 삭제 시 폴더 내 사진도 함께 삭제된다")
    fun givenFolderWithPhotos_whenDeleteFoldersWithDeletePhotosTrue_thenPhotosAlsoDeleted() {
        // Given: 폴더와 사진 생성
        val folder = folderRepository.save(Folder(userId = testUser.id!!, name = "사진 포함 폴더"))
        val media1 = createMedia(ownerId = testUser.id!!, status = MediaStatus.UPLOADED)
        val media2 = createMedia(ownerId = testUser.id!!, status = MediaStatus.UPLOADED)
        val photo1 = createPhotoImage(userId = testUser.id!!, mediaId = media1.id!!, folderId = folder.id)
        val photo2 = createPhotoImage(userId = testUser.id!!, mediaId = media2.id!!, folderId = folder.id)

        // When: deletePhotos=true로 폴더 삭제
        RestAssured.given()
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer $accessToken")
            .queryParam("deletePhotos", true)
            .body(DeleteFoldersRequest(folderIds = listOf(folder.id!!)))
            .`when`()
            .delete("/api/folders")
            .then()
            .statusCode(HttpStatus.OK.value())
            .body("resultCode", equalTo(ResultCode.SUCCESS.code))

        // Then: 폴더와 사진 모두 삭제됨
        assertThat(folderRepository.findById(folder.id!!)).isEmpty
        assertThat(photoImageRepository.findById(photo1.id!!)).isEmpty
        assertThat(photoImageRepository.findById(photo2.id!!)).isEmpty
    }

    @Test
    @DisplayName("deletePhotos=false(기본값)로 폴더 삭제 시 사진은 유지되고 연관관계만 해제된다")
    fun givenFolderWithPhotos_whenDeleteFoldersWithDeletePhotosFalse_thenPhotosRemainUnlinked() {
        // Given: 폴더와 사진 생성
        val folder = folderRepository.save(Folder(userId = testUser.id!!, name = "사진 포함 폴더"))
        val media = createMedia(ownerId = testUser.id!!, status = MediaStatus.UPLOADED)
        val photo = createPhotoImage(userId = testUser.id!!, mediaId = media.id!!, folderId = folder.id)

        // When: deletePhotos=false(기본값)로 폴더 삭제
        RestAssured.given()
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer $accessToken")
            .body(DeleteFoldersRequest(folderIds = listOf(folder.id!!)))
            .`when`()
            .delete("/api/folders")
            .then()
            .statusCode(HttpStatus.OK.value())
            .body("resultCode", equalTo(ResultCode.SUCCESS.code))

        // Then: 폴더는 삭제되고 사진은 유지 (folderId는 NULL)
        assertThat(folderRepository.findById(folder.id!!)).isEmpty
        val remainingPhoto = photoImageRepository.findById(photo.id!!).orElseThrow()
        assertThat(remainingPhoto.folderId).isNull()
    }

    @Test
    @DisplayName("deletePhotos=true로 여러 폴더 삭제 시 모든 폴더의 사진이 삭제된다")
    fun givenMultipleFoldersWithPhotos_whenDeleteFoldersWithDeletePhotosTrue_thenAllPhotosDeleted() {
        // Given: 여러 폴더와 각 폴더의 사진 생성
        val folder1 = folderRepository.save(Folder(userId = testUser.id!!, name = "폴더1"))
        val folder2 = folderRepository.save(Folder(userId = testUser.id!!, name = "폴더2"))

        val media1 = createMedia(ownerId = testUser.id!!, status = MediaStatus.UPLOADED)
        val media2 = createMedia(ownerId = testUser.id!!, status = MediaStatus.UPLOADED)
        val photo1 = createPhotoImage(userId = testUser.id!!, mediaId = media1.id!!, folderId = folder1.id)
        val photo2 = createPhotoImage(userId = testUser.id!!, mediaId = media2.id!!, folderId = folder2.id)

        // When
        RestAssured.given()
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer $accessToken")
            .queryParam("deletePhotos", true)
            .body(DeleteFoldersRequest(folderIds = listOf(folder1.id!!, folder2.id!!)))
            .`when`()
            .delete("/api/folders")
            .then()
            .statusCode(HttpStatus.OK.value())
            .body("resultCode", equalTo(ResultCode.SUCCESS.code))

        // Then
        assertThat(folderRepository.findById(folder1.id!!)).isEmpty
        assertThat(folderRepository.findById(folder2.id!!)).isEmpty
        assertThat(photoImageRepository.findById(photo1.id!!)).isEmpty
        assertThat(photoImageRepository.findById(photo2.id!!)).isEmpty
    }

    @Test
    @DisplayName("deletePhotos=true로 빈 폴더 삭제 시 정상 동작한다")
    fun givenEmptyFolder_whenDeleteFoldersWithDeletePhotosTrue_thenReturnsSuccess() {
        // Given: 사진 없는 빈 폴더 생성
        val folder = folderRepository.save(Folder(userId = testUser.id!!, name = "빈 폴더"))

        // When & Then
        RestAssured.given()
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer $accessToken")
            .queryParam("deletePhotos", true)
            .body(DeleteFoldersRequest(folderIds = listOf(folder.id!!)))
            .`when`()
            .delete("/api/folders")
            .then()
            .statusCode(HttpStatus.OK.value())
            .body("resultCode", equalTo(ResultCode.SUCCESS.code))

        assertThat(folderRepository.findById(folder.id!!)).isEmpty
    }

    @Test
    @DisplayName("deletePhotos=true로 즐겨찾기 사진이 포함된 폴더 삭제 시 즐겨찾기도 함께 삭제된다")
    fun givenFolderWithFavoritePhoto_whenDeleteFoldersWithDeletePhotosTrue_thenFavoriteAlsoDeleted() {
        // Given: 폴더와 즐겨찾기 사진 생성
        val folder = folderRepository.save(Folder(userId = testUser.id!!, name = "즐겨찾기 포함 폴더"))
        val media = createMedia(ownerId = testUser.id!!, status = MediaStatus.UPLOADED)
        val photo = createFavoritePhotoImage(userId = testUser.id!!, mediaId = media.id!!, folderId = folder.id)

        // When
        RestAssured.given()
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer $accessToken")
            .queryParam("deletePhotos", true)
            .body(DeleteFoldersRequest(folderIds = listOf(folder.id!!)))
            .`when`()
            .delete("/api/folders")
            .then()
            .statusCode(HttpStatus.OK.value())
            .body("resultCode", equalTo(ResultCode.SUCCESS.code))

        // Then
        assertThat(folderRepository.findById(folder.id!!)).isEmpty
        assertThat(photoImageRepository.findById(photo.id!!)).isEmpty
        assertThat(favoritePhotoRepository.countByIdUserId(testUser.id!!)).isEqualTo(0)
    }
}
