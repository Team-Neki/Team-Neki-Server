package com.neki.e2e.photo.folder

import com.neki.common.code.ResultCode
import com.neki.e2e.photo.image.PhotoImageE2ETestBase
import com.neki.media.entity.MediaStatus
import com.neki.photo.api.dto.FolderRequest
import com.neki.photo.entity.Folder
import com.neki.user.entity.User
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

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class MovePhotosToFolderE2ETest : PhotoImageE2ETestBase() {

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
    @DisplayName("사진을 source 폴더에서 target 폴더로 이동할 수 있다")
    fun givenPhotoInSourceFolder_whenMoveToTarget_thenPhotoMovedSuccessfully() {
        // Given
        val sourceFolder = folderRepository.save(Folder(userId = testUser.id!!, name = "소스 폴더"))
        val targetFolder = folderRepository.save(Folder(userId = testUser.id!!, name = "타겟 폴더"))
        val media = createMedia(ownerId = testUser.id!!, status = MediaStatus.UPLOADED)
        val photo = createPhotoImage(userId = testUser.id!!, mediaId = media.id!!, folderId = sourceFolder.id)

        // When
        RestAssured.given()
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer $accessToken")
            .body(
                FolderRequest.MovePhotosToFolder(
                    sourceFolderId = sourceFolder.id,
                    photoIds = listOf(photo.id!!),
                    targetFolderIds = listOf(targetFolder.id!!),
                ),
            )
            .`when`()
            .patch("/api/folders/photos/move")
            .then()
            .statusCode(HttpStatus.OK.value())
            .body("resultCode", equalTo(ResultCode.SUCCESS.code))

        // Then: source 폴더에서 사라지고 target 폴더에 존재
        val sourceLinks = photoImageFolderRepository.findAllByFolderIdIn(listOf(sourceFolder.id!!))
        assertThat(sourceLinks).isEmpty()

        val targetLinks = photoImageFolderRepository.findAllByFolderIdIn(listOf(targetFolder.id!!))
        assertThat(targetLinks).hasSize(1)
        assertThat(targetLinks[0].photoImageId).isEqualTo(photo.id)
    }

    @Test
    @DisplayName("여러 사진을 한번에 이동할 수 있다")
    fun givenMultiplePhotos_whenMoveToTarget_thenAllPhotosMoved() {
        // Given
        val sourceFolder = folderRepository.save(Folder(userId = testUser.id!!, name = "소스 폴더"))
        val targetFolder = folderRepository.save(Folder(userId = testUser.id!!, name = "타겟 폴더"))
        val media1 = createMedia(ownerId = testUser.id!!, status = MediaStatus.UPLOADED)
        val media2 = createMedia(ownerId = testUser.id!!, status = MediaStatus.UPLOADED)
        val media3 = createMedia(ownerId = testUser.id!!, status = MediaStatus.UPLOADED)
        val photo1 = createPhotoImage(userId = testUser.id!!, mediaId = media1.id!!, folderId = sourceFolder.id)
        val photo2 = createPhotoImage(userId = testUser.id!!, mediaId = media2.id!!, folderId = sourceFolder.id)
        val photo3 = createPhotoImage(userId = testUser.id!!, mediaId = media3.id!!, folderId = sourceFolder.id)

        // When
        RestAssured.given()
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer $accessToken")
            .body(
                FolderRequest.MovePhotosToFolder(
                    sourceFolderId = sourceFolder.id,
                    photoIds = listOf(photo1.id!!, photo2.id!!, photo3.id!!),
                    targetFolderIds = listOf(targetFolder.id!!),
                ),
            )
            .`when`()
            .patch("/api/folders/photos/move")
            .then()
            .statusCode(HttpStatus.OK.value())
            .body("resultCode", equalTo(ResultCode.SUCCESS.code))

        // Then
        val sourceLinks = photoImageFolderRepository.findAllByFolderIdIn(listOf(sourceFolder.id!!))
        assertThat(sourceLinks).isEmpty()

        val targetLinks = photoImageFolderRepository.findAllByFolderIdIn(listOf(targetFolder.id!!))
        assertThat(targetLinks).hasSize(3)
    }

    @Test
    @DisplayName("source와 target 폴더가 같으면 no-op으로 200을 반환한다")
    fun givenSameSourceAndTarget_whenMove_thenNoOpSuccess() {
        // Given
        val folder = folderRepository.save(Folder(userId = testUser.id!!, name = "폴더"))
        val media = createMedia(ownerId = testUser.id!!, status = MediaStatus.UPLOADED)
        val photo = createPhotoImage(userId = testUser.id!!, mediaId = media.id!!, folderId = folder.id)

        // When
        RestAssured.given()
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer $accessToken")
            .body(
                FolderRequest.MovePhotosToFolder(
                    sourceFolderId = folder.id,
                    photoIds = listOf(photo.id!!),
                    targetFolderIds = listOf(folder.id!!),
                ),
            )
            .`when`()
            .patch("/api/folders/photos/move")
            .then()
            .statusCode(HttpStatus.OK.value())
            .body("resultCode", equalTo(ResultCode.SUCCESS.code))

        // Then: 사진이 여전히 폴더에 존재
        val links = photoImageFolderRepository.findAllByFolderIdIn(listOf(folder.id!!))
        assertThat(links).hasSize(1)
        assertThat(links[0].photoImageId).isEqualTo(photo.id)
    }

    @Test
    @DisplayName("동일 요청을 두 번 보내도 멱등하게 200을 반환한다")
    fun givenAlreadyMoved_whenMoveAgain_thenIdempotentSuccess() {
        // Given
        val sourceFolder = folderRepository.save(Folder(userId = testUser.id!!, name = "소스 폴더"))
        val targetFolder = folderRepository.save(Folder(userId = testUser.id!!, name = "타겟 폴더"))
        val media = createMedia(ownerId = testUser.id!!, status = MediaStatus.UPLOADED)
        val photo = createPhotoImage(userId = testUser.id!!, mediaId = media.id!!, folderId = sourceFolder.id)

        val request = FolderRequest.MovePhotosToFolder(
            sourceFolderId = sourceFolder.id,
            photoIds = listOf(photo.id!!),
            targetFolderIds = listOf(targetFolder.id!!),
        )

        // When: 첫 번째 호출
        RestAssured.given()
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer $accessToken")
            .body(request)
            .`when`()
            .patch("/api/folders/photos/move")
            .then()
            .statusCode(HttpStatus.OK.value())

        // When: 두 번째 호출 (멱등성 확인)
        RestAssured.given()
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer $accessToken")
            .body(request)
            .`when`()
            .patch("/api/folders/photos/move")
            .then()
            .statusCode(HttpStatus.OK.value())
            .body("resultCode", equalTo(ResultCode.SUCCESS.code))

        // Then: target 폴더에 사진이 1개만 존재
        val targetLinks = photoImageFolderRepository.findAllByFolderIdIn(listOf(targetFolder.id!!))
        assertThat(targetLinks).hasSize(1)
        assertThat(targetLinks[0].photoImageId).isEqualTo(photo.id)
    }

    @Test
    @DisplayName("존재하지 않는 source 폴더로 요청 시 400 에러를 반환한다")
    fun givenNonExistentSourceFolder_whenMove_thenReturnsNotFound() {
        // Given
        val targetFolder = folderRepository.save(Folder(userId = testUser.id!!, name = "타겟 폴더"))
        val media = createMedia(ownerId = testUser.id!!, status = MediaStatus.UPLOADED)
        val photo = createPhotoImage(userId = testUser.id!!, mediaId = media.id!!)

        // When & Then
        RestAssured.given()
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer $accessToken")
            .body(
                FolderRequest.MovePhotosToFolder(
                    sourceFolderId = 99999L,
                    photoIds = listOf(photo.id!!),
                    targetFolderIds = listOf(targetFolder.id!!),
                ),
            )
            .`when`()
            .patch("/api/folders/photos/move")
            .then()
            .statusCode(HttpStatus.BAD_REQUEST.value())
            .body("resultCode", equalTo(ResultCode.NOT_FOUND.code))
    }

    @Test
    @DisplayName("존재하지 않는 target 폴더로 요청 시 400 에러를 반환한다")
    fun givenNonExistentTargetFolder_whenMove_thenReturnsNotFound() {
        // Given
        val sourceFolder = folderRepository.save(Folder(userId = testUser.id!!, name = "소스 폴더"))
        val media = createMedia(ownerId = testUser.id!!, status = MediaStatus.UPLOADED)
        val photo = createPhotoImage(userId = testUser.id!!, mediaId = media.id!!, folderId = sourceFolder.id)

        // When & Then
        RestAssured.given()
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer $accessToken")
            .body(
                FolderRequest.MovePhotosToFolder(
                    sourceFolderId = sourceFolder.id,
                    photoIds = listOf(photo.id!!),
                    targetFolderIds = listOf(99999L),
                ),
            )
            .`when`()
            .patch("/api/folders/photos/move")
            .then()
            .statusCode(HttpStatus.BAD_REQUEST.value())
            .body("resultCode", equalTo(ResultCode.NOT_FOUND.code))
    }

    @Test
    @DisplayName("다른 사용자의 source 폴더로 요청 시 400 에러를 반환한다")
    fun givenOtherUserSourceFolder_whenMove_thenReturnsNotFound() {
        // Given
        val (otherUser, _) = createTestUserAndToken(email = "other@example.com")
        val otherFolder = folderRepository.save(Folder(userId = otherUser.id!!, name = "다른 사용자 폴더"))
        val targetFolder = folderRepository.save(Folder(userId = testUser.id!!, name = "타겟 폴더"))
        val media = createMedia(ownerId = testUser.id!!, status = MediaStatus.UPLOADED)
        val photo = createPhotoImage(userId = testUser.id!!, mediaId = media.id!!)

        // When & Then
        RestAssured.given()
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer $accessToken")
            .body(
                FolderRequest.MovePhotosToFolder(
                    sourceFolderId = otherFolder.id,
                    photoIds = listOf(photo.id!!),
                    targetFolderIds = listOf(targetFolder.id!!),
                ),
            )
            .`when`()
            .patch("/api/folders/photos/move")
            .then()
            .statusCode(HttpStatus.BAD_REQUEST.value())
            .body("resultCode", equalTo(ResultCode.NOT_FOUND.code))
    }

    @Test
    @DisplayName("다른 사용자의 target 폴더로 요청 시 400 에러를 반환한다")
    fun givenOtherUserTargetFolder_whenMove_thenReturnsNotFound() {
        // Given
        val (otherUser, _) = createTestUserAndToken(email = "other@example.com")
        val sourceFolder = folderRepository.save(Folder(userId = testUser.id!!, name = "소스 폴더"))
        val otherFolder = folderRepository.save(Folder(userId = otherUser.id!!, name = "다른 사용자 폴더"))
        val media = createMedia(ownerId = testUser.id!!, status = MediaStatus.UPLOADED)
        val photo = createPhotoImage(userId = testUser.id!!, mediaId = media.id!!, folderId = sourceFolder.id)

        // When & Then
        RestAssured.given()
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer $accessToken")
            .body(
                FolderRequest.MovePhotosToFolder(
                    sourceFolderId = sourceFolder.id,
                    photoIds = listOf(photo.id!!),
                    targetFolderIds = listOf(otherFolder.id!!),
                ),
            )
            .`when`()
            .patch("/api/folders/photos/move")
            .then()
            .statusCode(HttpStatus.BAD_REQUEST.value())
            .body("resultCode", equalTo(ResultCode.NOT_FOUND.code))
    }

    @Test
    @DisplayName("빈 photoIds 리스트로 요청 시 400 에러를 반환한다")
    fun givenEmptyPhotoIds_whenMove_thenReturnsBadRequest() {
        // Given
        val sourceFolder = folderRepository.save(Folder(userId = testUser.id!!, name = "소스 폴더"))
        val targetFolder = folderRepository.save(Folder(userId = testUser.id!!, name = "타겟 폴더"))

        // When & Then
        RestAssured.given()
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer $accessToken")
            .body(
                FolderRequest.MovePhotosToFolder(
                    sourceFolderId = sourceFolder.id,
                    photoIds = emptyList(),
                    targetFolderIds = listOf(targetFolder.id!!),
                ),
            )
            .`when`()
            .patch("/api/folders/photos/move")
            .then()
            .statusCode(HttpStatus.BAD_REQUEST.value())
            .body("resultCode", equalTo(ResultCode.INVALID_PARAMETER.code))
    }

    @Test
    @DisplayName("source 폴더에 속하지 않은 사진을 이동 요청해도 성공을 반환한다")
    fun givenPhotoNotInSourceFolder_whenMove_thenSuccessAndPhotoAddedToTarget() {
        // Given: source 폴더에 속하지 않은 사진
        val sourceFolder = folderRepository.save(Folder(userId = testUser.id!!, name = "소스 폴더"))
        val targetFolder = folderRepository.save(Folder(userId = testUser.id!!, name = "타겟 폴더"))
        val media = createMedia(ownerId = testUser.id!!, status = MediaStatus.UPLOADED)
        val photoNotInSource = createPhotoImage(userId = testUser.id!!, mediaId = media.id!!, folderId = null)

        // When
        RestAssured.given()
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer $accessToken")
            .body(
                FolderRequest.MovePhotosToFolder(
                    sourceFolderId = sourceFolder.id,
                    photoIds = listOf(photoNotInSource.id!!),
                    targetFolderIds = listOf(targetFolder.id!!),
                ),
            )
            .`when`()
            .patch("/api/folders/photos/move")
            .then()
            .statusCode(HttpStatus.OK.value())
            .body("resultCode", equalTo(ResultCode.SUCCESS.code))

        // Then: target 폴더에 사진이 추가됨
        val targetLinks = photoImageFolderRepository.findAllByFolderIdIn(listOf(targetFolder.id!!))
        assertThat(targetLinks).hasSize(1)
        assertThat(targetLinks[0].photoImageId).isEqualTo(photoNotInSource.id)
    }

    @Test
    @DisplayName("source 폴더의 일부 사진만 이동하면 나머지는 source에 유지된다")
    fun givenPartialMove_whenMove_thenRemainingPhotosStayInSource() {
        // Given: source 폴더에 3개 사진, 그 중 2개만 이동
        val sourceFolder = folderRepository.save(Folder(userId = testUser.id!!, name = "소스 폴더"))
        val targetFolder = folderRepository.save(Folder(userId = testUser.id!!, name = "타겟 폴더"))
        val media1 = createMedia(ownerId = testUser.id!!, status = MediaStatus.UPLOADED)
        val media2 = createMedia(ownerId = testUser.id!!, status = MediaStatus.UPLOADED)
        val media3 = createMedia(ownerId = testUser.id!!, status = MediaStatus.UPLOADED)
        val photo1 = createPhotoImage(userId = testUser.id!!, mediaId = media1.id!!, folderId = sourceFolder.id)
        val photo2 = createPhotoImage(userId = testUser.id!!, mediaId = media2.id!!, folderId = sourceFolder.id)
        val photo3 = createPhotoImage(userId = testUser.id!!, mediaId = media3.id!!, folderId = sourceFolder.id)

        // When: photo1, photo2만 이동
        RestAssured.given()
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer $accessToken")
            .body(
                FolderRequest.MovePhotosToFolder(
                    sourceFolderId = sourceFolder.id,
                    photoIds = listOf(photo1.id!!, photo2.id!!),
                    targetFolderIds = listOf(targetFolder.id!!),
                ),
            )
            .`when`()
            .patch("/api/folders/photos/move")
            .then()
            .statusCode(HttpStatus.OK.value())
            .body("resultCode", equalTo(ResultCode.SUCCESS.code))

        // Then: photo3은 source에 유지, photo1/photo2는 target에 존재
        val sourceLinks = photoImageFolderRepository.findAllByFolderIdIn(listOf(sourceFolder.id!!))
        assertThat(sourceLinks).hasSize(1)
        assertThat(sourceLinks[0].photoImageId).isEqualTo(photo3.id)

        val targetLinks = photoImageFolderRepository.findAllByFolderIdIn(listOf(targetFolder.id!!))
        assertThat(targetLinks).hasSize(2)
        assertThat(targetLinks.map { it.photoImageId }).containsExactlyInAnyOrder(photo1.id, photo2.id)
    }

    @Test
    @DisplayName("target 폴더에 이미 사진이 있으면 기존 사진은 유지되고 이동된 사진이 추가된다")
    fun givenTargetHasExistingPhotos_whenMove_thenExistingPhotosPreserved() {
        // Given
        val sourceFolder = folderRepository.save(Folder(userId = testUser.id!!, name = "소스 폴더"))
        val targetFolder = folderRepository.save(Folder(userId = testUser.id!!, name = "타겟 폴더"))
        val media1 = createMedia(ownerId = testUser.id!!, status = MediaStatus.UPLOADED)
        val media2 = createMedia(ownerId = testUser.id!!, status = MediaStatus.UPLOADED)
        val existingPhoto = createPhotoImage(userId = testUser.id!!, mediaId = media1.id!!, folderId = targetFolder.id)
        val movingPhoto = createPhotoImage(userId = testUser.id!!, mediaId = media2.id!!, folderId = sourceFolder.id)

        // When
        RestAssured.given()
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer $accessToken")
            .body(
                FolderRequest.MovePhotosToFolder(
                    sourceFolderId = sourceFolder.id,
                    photoIds = listOf(movingPhoto.id!!),
                    targetFolderIds = listOf(targetFolder.id!!),
                ),
            )
            .`when`()
            .patch("/api/folders/photos/move")
            .then()
            .statusCode(HttpStatus.OK.value())
            .body("resultCode", equalTo(ResultCode.SUCCESS.code))

        // Then: target에 기존 사진 + 이동된 사진 모두 존재
        val targetLinks = photoImageFolderRepository.findAllByFolderIdIn(listOf(targetFolder.id!!))
        assertThat(targetLinks).hasSize(2)
        assertThat(targetLinks.map { it.photoImageId }).containsExactlyInAnyOrder(existingPhoto.id, movingPhoto.id)
    }

    @Test
    @DisplayName("모든 사진을 이동하면 source 폴더 커버가 NULL이 된다")
    fun givenAllPhotosMoved_whenMove_thenSourceCoverBecomesNull() {
        // Given
        val sourceFolder = folderRepository.save(Folder(userId = testUser.id!!, name = "소스 폴더"))
        val targetFolder = folderRepository.save(Folder(userId = testUser.id!!, name = "타겟 폴더"))
        val media = createMedia(ownerId = testUser.id!!, status = MediaStatus.UPLOADED)
        val photo = createPhotoImage(userId = testUser.id!!, mediaId = media.id!!, folderId = sourceFolder.id)

        // 이동 전 커버 확인
        val initialCoverUrl = getFolderLatestImageUrl(sourceFolder.id!!)
        assertThat(initialCoverUrl).isNotNull()

        // When
        RestAssured.given()
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer $accessToken")
            .body(
                FolderRequest.MovePhotosToFolder(
                    sourceFolderId = sourceFolder.id,
                    photoIds = listOf(photo.id!!),
                    targetFolderIds = listOf(targetFolder.id!!),
                ),
            )
            .`when`()
            .patch("/api/folders/photos/move")
            .then()
            .statusCode(HttpStatus.OK.value())

        // Then: source 폴더 커버가 NULL
        val sourceCoverUrl = getFolderLatestImageUrl(sourceFolder.id!!)
        assertThat(sourceCoverUrl).isNull()
    }

    @Test
    @DisplayName("사진 이동 후 target 폴더 커버가 업데이트된다")
    fun givenEmptyTargetFolder_whenMove_thenTargetCoverUpdated() {
        // Given: 빈 target 폴더
        val sourceFolder = folderRepository.save(Folder(userId = testUser.id!!, name = "소스 폴더"))
        val targetFolder = folderRepository.save(Folder(userId = testUser.id!!, name = "타겟 폴더"))
        val media = createMedia(ownerId = testUser.id!!, status = MediaStatus.UPLOADED)
        val photo = createPhotoImage(userId = testUser.id!!, mediaId = media.id!!, folderId = sourceFolder.id)

        // 이동 전 target 커버 확인
        val initialTargetCoverUrl = getFolderLatestImageUrl(targetFolder.id!!)
        assertThat(initialTargetCoverUrl).isNull()

        // When
        RestAssured.given()
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer $accessToken")
            .body(
                FolderRequest.MovePhotosToFolder(
                    sourceFolderId = sourceFolder.id,
                    photoIds = listOf(photo.id!!),
                    targetFolderIds = listOf(targetFolder.id!!),
                ),
            )
            .`when`()
            .patch("/api/folders/photos/move")
            .then()
            .statusCode(HttpStatus.OK.value())

        // Then: target 폴더 커버가 업데이트됨
        val updatedTargetCoverUrl = getFolderLatestImageUrl(targetFolder.id!!)
        assertThat(updatedTargetCoverUrl).isNotNull()
    }

    // ===================
    // Helper Methods
    // ===================

    private fun getFolderLatestImageUrl(folderId: Long): String? {
        val response = RestAssured.given()
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer $accessToken")
            .`when`()
            .get("/api/folders")
            .then()
            .statusCode(HttpStatus.OK.value())
            .extract()

        val items = response.jsonPath().getList<Map<String, Any>>("data.items")
        val folderData = items.find { (it["folderId"] as Number).toLong() == folderId }
            ?: throw AssertionError("Folder with id $folderId not found")

        return folderData["latestImageUrl"] as String?
    }
}
