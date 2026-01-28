package com.yapp2app.e2e.photo.folder

import com.yapp2app.common.api.dto.ResultCode
import com.yapp2app.e2e.photo.image.PhotoImageE2ETestBase
import com.yapp2app.media.domain.entity.MediaStatus
import com.yapp2app.photo.api.dto.RemovePhotosFromFolderRequest
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
 * fileName       : RemovePhotosFromFolderE2ETest
 * author         : claude
 * date           : 2026. 1. 28.
 * description    : 폴더에서 사진 제외 E2E 테스트 (시나리오 4: 연관관계만 해제)
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class RemovePhotosFromFolderE2ETest : PhotoImageE2ETestBase() {

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
    @DisplayName("폴더에서 사진 제외 성공 시 사진의 folderId가 NULL이 된다")
    fun givenPhotosInFolder_whenRemovePhotosFromFolder_thenPhotosFolderIdBecomesNull() {
        // Given: 폴더와 사진 생성
        val folder = folderRepository.save(Folder(userId = testUser.id!!, name = "테스트 폴더"))
        val media = createMedia(ownerId = testUser.id!!, status = MediaStatus.UPLOADED)
        val photo = createPhotoImage(userId = testUser.id!!, mediaId = media.id!!, folderId = folder.id)

        // When
        RestAssured.given()
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer $accessToken")
            .body(RemovePhotosFromFolderRequest(photoIds = listOf(photo.id!!)))
            .`when`()
            .delete("/api/folders/${folder.id}/photos")
            .then()
            .statusCode(HttpStatus.OK.value())
            .body("resultCode", equalTo(ResultCode.SUCCESS.code))

        // Then: 사진은 유지되고 folderId만 NULL로 변경
        val remainingPhoto = photoImageRepository.findById(photo.id!!).orElseThrow()
        assertThat(remainingPhoto.folderId).isNull()
        assertThat(remainingPhoto.mediaId).isEqualTo(media.id!!)
    }

    @Test
    @DisplayName("여러 사진을 한번에 폴더에서 제외할 수 있다")
    fun givenMultiplePhotosInFolder_whenRemovePhotosFromFolder_thenAllPhotosUnlinked() {
        // Given: 폴더와 여러 사진 생성
        val folder = folderRepository.save(Folder(userId = testUser.id!!, name = "테스트 폴더"))
        val media1 = createMedia(ownerId = testUser.id!!, status = MediaStatus.UPLOADED)
        val media2 = createMedia(ownerId = testUser.id!!, status = MediaStatus.UPLOADED)
        val media3 = createMedia(ownerId = testUser.id!!, status = MediaStatus.UPLOADED)
        val photo1 = createPhotoImage(userId = testUser.id!!, mediaId = media1.id!!, folderId = folder.id)
        val photo2 = createPhotoImage(userId = testUser.id!!, mediaId = media2.id!!, folderId = folder.id)
        val photo3 = createPhotoImage(userId = testUser.id!!, mediaId = media3.id!!, folderId = folder.id)

        // When
        RestAssured.given()
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer $accessToken")
            .body(RemovePhotosFromFolderRequest(photoIds = listOf(photo1.id!!, photo2.id!!, photo3.id!!)))
            .`when`()
            .delete("/api/folders/${folder.id}/photos")
            .then()
            .statusCode(HttpStatus.OK.value())
            .body("resultCode", equalTo(ResultCode.SUCCESS.code))

        // Then: 모든 사진의 folderId가 NULL로 변경
        assertThat(photoImageRepository.findById(photo1.id!!).get().folderId).isNull()
        assertThat(photoImageRepository.findById(photo2.id!!).get().folderId).isNull()
        assertThat(photoImageRepository.findById(photo3.id!!).get().folderId).isNull()
    }

    @Test
    @DisplayName("존재하지 않는 폴더에서 사진 제외 시 404 에러를 반환한다")
    fun givenNonExistentFolder_whenRemovePhotosFromFolder_thenReturnsNotFound() {
        // Given: 사진만 생성 (폴더 없음)
        val media = createMedia(ownerId = testUser.id!!, status = MediaStatus.UPLOADED)
        val photo = createPhotoImage(userId = testUser.id!!, mediaId = media.id!!)
        val nonExistentFolderId = 99999L

        // When & Then
        RestAssured.given()
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer $accessToken")
            .body(RemovePhotosFromFolderRequest(photoIds = listOf(photo.id!!)))
            .`when`()
            .delete("/api/folders/$nonExistentFolderId/photos")
            .then()
            .statusCode(HttpStatus.BAD_REQUEST.value())
            .body("resultCode", equalTo(ResultCode.NOT_FOUND.code))
    }

    @Test
    @DisplayName("다른 사용자의 폴더에서 사진 제외 시 404 에러를 반환한다")
    fun givenOtherUserFolder_whenRemovePhotosFromFolder_thenReturnsNotFound() {
        // Given: 다른 사용자의 폴더 생성
        val (otherUser, _) = createTestUserAndToken(email = "other@example.com")
        val otherFolder = folderRepository.save(Folder(userId = otherUser.id!!, name = "다른 사용자 폴더"))
        val media = createMedia(ownerId = testUser.id!!, status = MediaStatus.UPLOADED)
        val photo = createPhotoImage(userId = testUser.id!!, mediaId = media.id!!)

        // When & Then
        RestAssured.given()
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer $accessToken")
            .body(RemovePhotosFromFolderRequest(photoIds = listOf(photo.id!!)))
            .`when`()
            .delete("/api/folders/${otherFolder.id}/photos")
            .then()
            .statusCode(HttpStatus.BAD_REQUEST.value())
            .body("resultCode", equalTo(ResultCode.NOT_FOUND.code))
    }

    @Test
    @DisplayName("빈 photoIds 리스트로 요청 시 400 에러를 반환한다")
    fun givenEmptyPhotoIds_whenRemovePhotosFromFolder_thenReturnsBadRequest() {
        // Given: 폴더 생성
        val folder = folderRepository.save(Folder(userId = testUser.id!!, name = "테스트 폴더"))

        // When & Then
        RestAssured.given()
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer $accessToken")
            .body(RemovePhotosFromFolderRequest(photoIds = emptyList()))
            .`when`()
            .delete("/api/folders/${folder.id}/photos")
            .then()
            .statusCode(HttpStatus.BAD_REQUEST.value())
            .body("resultCode", equalTo(ResultCode.INVALID_PARAMETER.code))
    }

    @Test
    @DisplayName("폴더에 속하지 않은 사진을 제외 요청해도 성공을 반환한다")
    fun givenPhotoNotInFolder_whenRemovePhotosFromFolder_thenReturnsSuccess() {
        // Given: 폴더와 폴더에 속하지 않은 사진 생성
        val folder = folderRepository.save(Folder(userId = testUser.id!!, name = "테스트 폴더"))
        val media = createMedia(ownerId = testUser.id!!, status = MediaStatus.UPLOADED)
        val photoNotInFolder = createPhotoImage(userId = testUser.id!!, mediaId = media.id!!, folderId = null)

        // When & Then: 폴더에 속하지 않은 사진은 무시됨
        RestAssured.given()
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer $accessToken")
            .body(RemovePhotosFromFolderRequest(photoIds = listOf(photoNotInFolder.id!!)))
            .`when`()
            .delete("/api/folders/${folder.id}/photos")
            .then()
            .statusCode(HttpStatus.OK.value())
            .body("resultCode", equalTo(ResultCode.SUCCESS.code))
    }

    @Test
    @DisplayName("다른 폴더에 속한 사진을 제외 요청해도 성공을 반환한다 (해당 사진은 변경되지 않음)")
    fun givenPhotoInDifferentFolder_whenRemovePhotosFromFolder_thenPhotoRemainUnchanged() {
        // Given: 두 개의 폴더와 각각에 속한 사진 생성
        val folder1 = folderRepository.save(Folder(userId = testUser.id!!, name = "폴더1"))
        val folder2 = folderRepository.save(Folder(userId = testUser.id!!, name = "폴더2"))
        val media = createMedia(ownerId = testUser.id!!, status = MediaStatus.UPLOADED)
        val photoInFolder2 = createPhotoImage(userId = testUser.id!!, mediaId = media.id!!, folderId = folder2.id)

        // When: folder1에서 folder2의 사진 제외 요청
        RestAssured.given()
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer $accessToken")
            .body(RemovePhotosFromFolderRequest(photoIds = listOf(photoInFolder2.id!!)))
            .`when`()
            .delete("/api/folders/${folder1.id}/photos")
            .then()
            .statusCode(HttpStatus.OK.value())
            .body("resultCode", equalTo(ResultCode.SUCCESS.code))

        // Then: 사진은 여전히 folder2에 속함
        val photo = photoImageRepository.findById(photoInFolder2.id!!).orElseThrow()
        assertThat(photo.folderId).isEqualTo(folder2.id)
    }

    @Test
    @DisplayName("커버 사진을 제외하면 커버가 자동으로 재계산된다")
    fun givenCoverPhoto_whenRemovePhotosFromFolder_thenCoverRecalculated() {
        // Given: 폴더와 여러 사진 생성 (첫 번째 사진이 커버)
        val folder = folderRepository.save(Folder(userId = testUser.id!!, name = "테스트 폴더"))
        val media1 = createMedia(ownerId = testUser.id!!, status = MediaStatus.UPLOADED)
        val media2 = createMedia(ownerId = testUser.id!!, status = MediaStatus.UPLOADED)
        val photo1 = createPhotoImage(userId = testUser.id!!, mediaId = media1.id!!, folderId = folder.id)
        Thread.sleep(10) // 시간차를 두어 photo2가 더 최신이 되도록
        val photo2 = createPhotoImage(userId = testUser.id!!, mediaId = media2.id!!, folderId = folder.id)

        // 커버를 photo2로 설정
        folder.coverPhotoId = photo2.id
        folderRepository.save(folder)

        // When: 커버 사진(photo2) 제외
        RestAssured.given()
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer $accessToken")
            .body(RemovePhotosFromFolderRequest(photoIds = listOf(photo2.id!!)))
            .`when`()
            .delete("/api/folders/${folder.id}/photos")
            .then()
            .statusCode(HttpStatus.OK.value())
            .body("resultCode", equalTo(ResultCode.SUCCESS.code))

        // Then: 커버가 재계산됨 (photo1으로 변경되거나 NULL)
        val updatedFolder = folderRepository.findById(folder.id!!).orElseThrow()
        assertThat(updatedFolder.coverPhotoId).isNotEqualTo(photo2.id)
    }

    @Test
    @DisplayName("모든 사진을 제외하면 커버가 NULL이 된다")
    fun givenAllPhotosRemoved_whenRemovePhotosFromFolder_thenCoverBecomesNull() {
        // Given: 폴더와 단일 사진 생성
        val folder = folderRepository.save(Folder(userId = testUser.id!!, name = "테스트 폴더"))
        val media = createMedia(ownerId = testUser.id!!, status = MediaStatus.UPLOADED)
        val photo = createPhotoImage(userId = testUser.id!!, mediaId = media.id!!, folderId = folder.id)
        folder.coverPhotoId = photo.id
        folderRepository.save(folder)

        // When: 유일한 사진 제외
        RestAssured.given()
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer $accessToken")
            .body(RemovePhotosFromFolderRequest(photoIds = listOf(photo.id!!)))
            .`when`()
            .delete("/api/folders/${folder.id}/photos")
            .then()
            .statusCode(HttpStatus.OK.value())
            .body("resultCode", equalTo(ResultCode.SUCCESS.code))

        // Then: 커버가 NULL이 됨
        val updatedFolder = folderRepository.findById(folder.id!!).orElseThrow()
        assertThat(updatedFolder.coverPhotoId).isNull()
    }
}
