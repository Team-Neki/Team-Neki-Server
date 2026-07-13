package com.neki.e2e.photo.folder

import com.neki.common.code.ResultCode
import com.neki.e2e.photo.image.PhotoImageE2ETestBase
import com.neki.media.domain.entity.MediaStatus
import com.neki.photo.api.dto.CopyPhotosToFolderRequest
import com.neki.photo.domain.entity.Folder
import com.neki.user.domain.entity.User
import io.restassured.RestAssured
import io.restassured.http.ContentType
import io.restassured.specification.RequestSpecification
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
class CopyPhotosToFolderE2ETest : PhotoImageE2ETestBase() {

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

    // ===================
    // 기본 테스트
    // ===================

    @Test
    @DisplayName("단일 사진을 복제하면 source 유지, target 추가")
    fun givenPhotoInSourceFolder_whenCopyToTarget_thenPhotoExistsInBoth() {
        // Given
        val sourceFolder = folderRepository.save(Folder(userId = testUser.id!!, name = "소스 폴더"))
        val targetFolder = folderRepository.save(Folder(userId = testUser.id!!, name = "타겟 폴더"))
        val media = createMedia(ownerId = testUser.id!!, status = MediaStatus.UPLOADED)
        val photo = createPhotoImage(userId = testUser.id!!, mediaId = media.id!!, folderId = sourceFolder.id)

        // When
        givenAuthenticated()
            .body(
                CopyPhotosToFolderRequest(
                    photoIds = listOf(photo.id!!),
                    targetFolderIds = listOf(targetFolder.id!!),
                ),
            )
            .`when`()
            .post("/api/folders/photos/copy")
            .then()
            .statusCode(HttpStatus.OK.value())
            .body("resultCode", equalTo(ResultCode.SUCCESS.code))

        // Then: source 폴더에 유지, target 폴더에도 존재
        val sourceLinks = photoImageFolderRepository.findAllByFolderIdIn(listOf(sourceFolder.id!!))
        assertThat(sourceLinks).hasSize(1)
        assertThat(sourceLinks[0].photoImageId).isEqualTo(photo.id)

        val targetLinks = photoImageFolderRepository.findAllByFolderIdIn(listOf(targetFolder.id!!))
        assertThat(targetLinks).hasSize(1)
        assertThat(targetLinks[0].photoImageId).isEqualTo(photo.id)
    }

    @Test
    @DisplayName("여러 사진을 한번에 복제할 수 있다")
    fun givenMultiplePhotos_whenCopyToTarget_thenAllPhotosCopied() {
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
        givenAuthenticated()
            .body(
                CopyPhotosToFolderRequest(
                    photoIds = listOf(photo1.id!!, photo2.id!!, photo3.id!!),
                    targetFolderIds = listOf(targetFolder.id!!),
                ),
            )
            .`when`()
            .post("/api/folders/photos/copy")
            .then()
            .statusCode(HttpStatus.OK.value())
            .body("resultCode", equalTo(ResultCode.SUCCESS.code))

        // Then: source 유지, target 추가
        val sourceLinks = photoImageFolderRepository.findAllByFolderIdIn(listOf(sourceFolder.id!!))
        assertThat(sourceLinks).hasSize(3)

        val targetLinks = photoImageFolderRepository.findAllByFolderIdIn(listOf(targetFolder.id!!))
        assertThat(targetLinks).hasSize(3)
    }

    @Test
    @DisplayName("source와 target 폴더가 같으면 no-op으로 200을 반환한다")
    fun givenSameSourceAndTarget_whenCopy_thenNoOpSuccess() {
        // Given
        val folder = folderRepository.save(Folder(userId = testUser.id!!, name = "폴더"))
        val media = createMedia(ownerId = testUser.id!!, status = MediaStatus.UPLOADED)
        val photo = createPhotoImage(userId = testUser.id!!, mediaId = media.id!!, folderId = folder.id)

        // When
        givenAuthenticated()
            .body(
                CopyPhotosToFolderRequest(
                    photoIds = listOf(photo.id!!),
                    targetFolderIds = listOf(folder.id!!),
                ),
            )
            .`when`()
            .post("/api/folders/photos/copy")
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
    fun givenAlreadyCopied_whenCopyAgain_thenIdempotentSuccess() {
        // Given
        val sourceFolder = folderRepository.save(Folder(userId = testUser.id!!, name = "소스 폴더"))
        val targetFolder = folderRepository.save(Folder(userId = testUser.id!!, name = "타겟 폴더"))
        val media = createMedia(ownerId = testUser.id!!, status = MediaStatus.UPLOADED)
        val photo = createPhotoImage(userId = testUser.id!!, mediaId = media.id!!, folderId = sourceFolder.id)

        val request = CopyPhotosToFolderRequest(
            photoIds = listOf(photo.id!!),
            targetFolderIds = listOf(targetFolder.id!!),
        )

        // When: 첫 번째 호출
        givenAuthenticated()
            .body(request)
            .`when`()
            .post("/api/folders/photos/copy")
            .then()
            .statusCode(HttpStatus.OK.value())

        // When: 두 번째 호출 (멱등성 확인)
        givenAuthenticated()
            .body(request)
            .`when`()
            .post("/api/folders/photos/copy")
            .then()
            .statusCode(HttpStatus.OK.value())
            .body("resultCode", equalTo(ResultCode.SUCCESS.code))

        // Then: target 폴더에 사진이 1개만 존재, source도 유지
        val sourceLinks = photoImageFolderRepository.findAllByFolderIdIn(listOf(sourceFolder.id!!))
        assertThat(sourceLinks).hasSize(1)

        val targetLinks = photoImageFolderRepository.findAllByFolderIdIn(listOf(targetFolder.id!!))
        assertThat(targetLinks).hasSize(1)
        assertThat(targetLinks[0].photoImageId).isEqualTo(photo.id)
    }

    // ===================
    // 에러 테스트
    // ===================

    @Test
    @DisplayName("존재하지 않는 target 폴더로 요청 시 400 에러를 반환한다")
    fun givenNonExistentTargetFolder_whenCopy_thenReturnsNotFound() {
        // Given
        val sourceFolder = folderRepository.save(Folder(userId = testUser.id!!, name = "소스 폴더"))
        val media = createMedia(ownerId = testUser.id!!, status = MediaStatus.UPLOADED)
        val photo = createPhotoImage(userId = testUser.id!!, mediaId = media.id!!, folderId = sourceFolder.id)

        // When & Then
        givenAuthenticated()
            .body(
                CopyPhotosToFolderRequest(
                    photoIds = listOf(photo.id!!),
                    targetFolderIds = listOf(99999L),
                ),
            )
            .`when`()
            .post("/api/folders/photos/copy")
            .then()
            .statusCode(HttpStatus.BAD_REQUEST.value())
            .body("resultCode", equalTo(ResultCode.NOT_FOUND.code))
    }

    @Test
    @DisplayName("다른 사용자의 target 폴더로 요청 시 400 에러를 반환한다")
    fun givenOtherUserTargetFolder_whenCopy_thenReturnsNotFound() {
        // Given
        val (otherUser, _) = createTestUserAndToken(email = "other@example.com")
        val sourceFolder = folderRepository.save(Folder(userId = testUser.id!!, name = "소스 폴더"))
        val otherFolder = folderRepository.save(Folder(userId = otherUser.id!!, name = "다른 사용자 폴더"))
        val media = createMedia(ownerId = testUser.id!!, status = MediaStatus.UPLOADED)
        val photo = createPhotoImage(userId = testUser.id!!, mediaId = media.id!!, folderId = sourceFolder.id)

        // When & Then
        givenAuthenticated()
            .body(
                CopyPhotosToFolderRequest(
                    photoIds = listOf(photo.id!!),
                    targetFolderIds = listOf(otherFolder.id!!),
                ),
            )
            .`when`()
            .post("/api/folders/photos/copy")
            .then()
            .statusCode(HttpStatus.BAD_REQUEST.value())
            .body("resultCode", equalTo(ResultCode.NOT_FOUND.code))
    }

    @Test
    @DisplayName("다른 사용자의 사진을 복제하려고 하면 400 에러를 반환한다")
    fun givenOtherUserPhoto_whenCopy_thenReturnsNotFound() {
        // Given
        val (otherUser, _) = createTestUserAndToken(email = "other@example.com")
        val sourceFolder = folderRepository.save(Folder(userId = testUser.id!!, name = "소스 폴더"))
        val targetFolder = folderRepository.save(Folder(userId = testUser.id!!, name = "타겟 폴더"))

        val otherUserMedia = createMedia(ownerId = otherUser.id!!, status = MediaStatus.UPLOADED)
        val otherUserPhoto = createPhotoImage(userId = otherUser.id!!, mediaId = otherUserMedia.id!!)

        // When & Then
        givenAuthenticated()
            .body(
                CopyPhotosToFolderRequest(
                    photoIds = listOf(otherUserPhoto.id!!),
                    targetFolderIds = listOf(targetFolder.id!!),
                ),
            )
            .`when`()
            .post("/api/folders/photos/copy")
            .then()
            .statusCode(HttpStatus.BAD_REQUEST.value())
            .body("resultCode", equalTo(ResultCode.NOT_FOUND.code))
    }

    @Test
    @DisplayName("빈 photoIds 리스트로 요청 시 400 에러를 반환한다")
    fun givenEmptyPhotoIds_whenCopy_thenReturnsBadRequest() {
        // Given
        val sourceFolder = folderRepository.save(Folder(userId = testUser.id!!, name = "소스 폴더"))
        val targetFolder = folderRepository.save(Folder(userId = testUser.id!!, name = "타겟 폴더"))

        // When & Then
        givenAuthenticated()
            .body(
                CopyPhotosToFolderRequest(
                    photoIds = emptyList(),
                    targetFolderIds = listOf(targetFolder.id!!),
                ),
            )
            .`when`()
            .post("/api/folders/photos/copy")
            .then()
            .statusCode(HttpStatus.BAD_REQUEST.value())
            .body("resultCode", equalTo(ResultCode.INVALID_PARAMETER.code))
    }

    // ===================
    // 엣지 케이스
    // ===================

    @Test
    @DisplayName("source 폴더에 속하지 않은 사진도 target에 추가된다")
    fun givenPhotoNotInSourceFolder_whenCopy_thenSuccessAndPhotoAddedToTarget() {
        // Given
        val sourceFolder = folderRepository.save(Folder(userId = testUser.id!!, name = "소스 폴더"))
        val targetFolder = folderRepository.save(Folder(userId = testUser.id!!, name = "타겟 폴더"))
        val media = createMedia(ownerId = testUser.id!!, status = MediaStatus.UPLOADED)
        val photoNotInSource = createPhotoImage(userId = testUser.id!!, mediaId = media.id!!, folderId = null)

        // When
        givenAuthenticated()
            .body(
                CopyPhotosToFolderRequest(
                    photoIds = listOf(photoNotInSource.id!!),
                    targetFolderIds = listOf(targetFolder.id!!),
                ),
            )
            .`when`()
            .post("/api/folders/photos/copy")
            .then()
            .statusCode(HttpStatus.OK.value())
            .body("resultCode", equalTo(ResultCode.SUCCESS.code))

        // Then: target 폴더에 사진이 추가됨
        val targetLinks = photoImageFolderRepository.findAllByFolderIdIn(listOf(targetFolder.id!!))
        assertThat(targetLinks).hasSize(1)
        assertThat(targetLinks[0].photoImageId).isEqualTo(photoNotInSource.id)
    }

    @Test
    @DisplayName("source 폴더의 일부 사진만 복제하면 source 전체 유지, target에 일부 추가")
    fun givenPartialCopy_whenCopy_thenSourcePreservedAndTargetHasSubset() {
        // Given
        val sourceFolder = folderRepository.save(Folder(userId = testUser.id!!, name = "소스 폴더"))
        val targetFolder = folderRepository.save(Folder(userId = testUser.id!!, name = "타겟 폴더"))
        val media1 = createMedia(ownerId = testUser.id!!, status = MediaStatus.UPLOADED)
        val media2 = createMedia(ownerId = testUser.id!!, status = MediaStatus.UPLOADED)
        val media3 = createMedia(ownerId = testUser.id!!, status = MediaStatus.UPLOADED)
        val photo1 = createPhotoImage(userId = testUser.id!!, mediaId = media1.id!!, folderId = sourceFolder.id)
        val photo2 = createPhotoImage(userId = testUser.id!!, mediaId = media2.id!!, folderId = sourceFolder.id)
        val photo3 = createPhotoImage(userId = testUser.id!!, mediaId = media3.id!!, folderId = sourceFolder.id)

        // When: photo1, photo2만 복제
        givenAuthenticated()
            .body(
                CopyPhotosToFolderRequest(
                    photoIds = listOf(photo1.id!!, photo2.id!!),
                    targetFolderIds = listOf(targetFolder.id!!),
                ),
            )
            .`when`()
            .post("/api/folders/photos/copy")
            .then()
            .statusCode(HttpStatus.OK.value())
            .body("resultCode", equalTo(ResultCode.SUCCESS.code))

        // Then: source 3개 모두 유지, target에 2개 추가
        val sourceLinks = photoImageFolderRepository.findAllByFolderIdIn(listOf(sourceFolder.id!!))
        assertThat(sourceLinks).hasSize(3)
        assertThat(sourceLinks.map { it.photoImageId }).containsExactlyInAnyOrder(photo1.id, photo2.id, photo3.id)

        val targetLinks = photoImageFolderRepository.findAllByFolderIdIn(listOf(targetFolder.id!!))
        assertThat(targetLinks).hasSize(2)
        assertThat(targetLinks.map { it.photoImageId }).containsExactlyInAnyOrder(photo1.id, photo2.id)
    }

    @Test
    @DisplayName("target 폴더에 이미 사진이 있으면 기존 사진은 유지되고 복제된 사진이 추가된다")
    fun givenTargetHasExistingPhotos_whenCopy_thenExistingPhotosPreserved() {
        // Given
        val sourceFolder = folderRepository.save(Folder(userId = testUser.id!!, name = "소스 폴더"))
        val targetFolder = folderRepository.save(Folder(userId = testUser.id!!, name = "타겟 폴더"))
        val media1 = createMedia(ownerId = testUser.id!!, status = MediaStatus.UPLOADED)
        val media2 = createMedia(ownerId = testUser.id!!, status = MediaStatus.UPLOADED)
        val existingPhoto = createPhotoImage(userId = testUser.id!!, mediaId = media1.id!!, folderId = targetFolder.id)
        val copyingPhoto = createPhotoImage(userId = testUser.id!!, mediaId = media2.id!!, folderId = sourceFolder.id)

        // When
        givenAuthenticated()
            .body(
                CopyPhotosToFolderRequest(
                    photoIds = listOf(copyingPhoto.id!!),
                    targetFolderIds = listOf(targetFolder.id!!),
                ),
            )
            .`when`()
            .post("/api/folders/photos/copy")
            .then()
            .statusCode(HttpStatus.OK.value())
            .body("resultCode", equalTo(ResultCode.SUCCESS.code))

        // Then: target에 기존 사진 + 복제된 사진 모두 존재
        val targetLinks = photoImageFolderRepository.findAllByFolderIdIn(listOf(targetFolder.id!!))
        assertThat(targetLinks).hasSize(2)
        assertThat(targetLinks.map { it.photoImageId }).containsExactlyInAnyOrder(existingPhoto.id, copyingPhoto.id)
    }

    @Test
    @DisplayName("복제 후 target 폴더 커버가 업데이트된다")
    fun givenEmptyTargetFolder_whenCopy_thenTargetCoverUpdated() {
        // Given
        val sourceFolder = folderRepository.save(Folder(userId = testUser.id!!, name = "소스 폴더"))
        val targetFolder = folderRepository.save(Folder(userId = testUser.id!!, name = "타겟 폴더"))
        val media = createMedia(ownerId = testUser.id!!, status = MediaStatus.UPLOADED)
        val photo = createPhotoImage(userId = testUser.id!!, mediaId = media.id!!, folderId = sourceFolder.id)

        // 복제 전 target 커버 확인
        val initialTargetCoverUrl = getFolderLatestImageUrl(targetFolder.id!!)
        assertThat(initialTargetCoverUrl).isNull()

        // When
        givenAuthenticated()
            .body(
                CopyPhotosToFolderRequest(
                    photoIds = listOf(photo.id!!),
                    targetFolderIds = listOf(targetFolder.id!!),
                ),
            )
            .`when`()
            .post("/api/folders/photos/copy")
            .then()
            .statusCode(HttpStatus.OK.value())

        // Then: target 폴더 커버가 업데이트됨
        val updatedTargetCoverUrl = getFolderLatestImageUrl(targetFolder.id!!)
        assertThat(updatedTargetCoverUrl).isNotNull()
    }

    @Test
    @DisplayName("복제 후 source 폴더 사진과 커버는 변경되지 않는다")
    fun givenPhotoInSource_whenCopy_thenSourceUnchanged() {
        // Given
        val sourceFolder = folderRepository.save(Folder(userId = testUser.id!!, name = "소스 폴더"))
        val targetFolder = folderRepository.save(Folder(userId = testUser.id!!, name = "타겟 폴더"))
        val media = createMedia(ownerId = testUser.id!!, status = MediaStatus.UPLOADED)
        val photo = createPhotoImage(userId = testUser.id!!, mediaId = media.id!!, folderId = sourceFolder.id)

        // 복제 전 source 커버 확인
        val initialSourceCoverUrl = getFolderLatestImageUrl(sourceFolder.id!!)
        assertThat(initialSourceCoverUrl).isNotNull()

        // When
        givenAuthenticated()
            .body(
                CopyPhotosToFolderRequest(
                    photoIds = listOf(photo.id!!),
                    targetFolderIds = listOf(targetFolder.id!!),
                ),
            )
            .`when`()
            .post("/api/folders/photos/copy")
            .then()
            .statusCode(HttpStatus.OK.value())

        // Then: source 폴더 사진과 커버 변경 없음
        val sourceLinks = photoImageFolderRepository.findAllByFolderIdIn(listOf(sourceFolder.id!!))
        assertThat(sourceLinks).hasSize(1)
        assertThat(sourceLinks[0].photoImageId).isEqualTo(photo.id)

        val sourceCoverUrl = getFolderLatestImageUrl(sourceFolder.id!!)
        assertThat(sourceCoverUrl).isEqualTo(initialSourceCoverUrl)
    }

    @Test
    @DisplayName("target에 복제 대상 중 일부가 이미 존재하면 중복 없이 모두 추가된다")
    fun givenTargetHasPartialOverlap_whenCopy_thenNoDuplicatesAndAllPresent() {
        // Given
        val sourceFolder = folderRepository.save(Folder(userId = testUser.id!!, name = "소스 폴더"))
        val targetFolder = folderRepository.save(Folder(userId = testUser.id!!, name = "타겟 폴더"))
        val media1 = createMedia(ownerId = testUser.id!!, status = MediaStatus.UPLOADED)
        val media2 = createMedia(ownerId = testUser.id!!, status = MediaStatus.UPLOADED)
        val media3 = createMedia(ownerId = testUser.id!!, status = MediaStatus.UPLOADED)
        val photo1 = createPhotoImage(userId = testUser.id!!, mediaId = media1.id!!, folderId = sourceFolder.id)
        val photo2 = createPhotoImage(userId = testUser.id!!, mediaId = media2.id!!, folderId = sourceFolder.id)
        val existingOther = createPhotoImage(userId = testUser.id!!, mediaId = media3.id!!, folderId = targetFolder.id)

        // photo1을 target에도 미리 추가 (partial overlap 상태)
        givenAuthenticated()
            .body(
                CopyPhotosToFolderRequest(
                    photoIds = listOf(photo1.id!!),
                    targetFolderIds = listOf(targetFolder.id!!),
                ),
            )
            .`when`()
            .post("/api/folders/photos/copy")
            .then()
            .statusCode(HttpStatus.OK.value())

        // When: photo1 + photo2를 복제 (photo1은 이미 target에 존재)
        givenAuthenticated()
            .body(
                CopyPhotosToFolderRequest(
                    photoIds = listOf(photo1.id!!, photo2.id!!),
                    targetFolderIds = listOf(targetFolder.id!!),
                ),
            )
            .`when`()
            .post("/api/folders/photos/copy")
            .then()
            .statusCode(HttpStatus.OK.value())
            .body("resultCode", equalTo(ResultCode.SUCCESS.code))

        // Then: target에 existingOther, photo1, photo2 = 3개 (중복 없음)
        val targetLinks = photoImageFolderRepository.findAllByFolderIdIn(listOf(targetFolder.id!!))
        assertThat(targetLinks).hasSize(3)
        assertThat(targetLinks.map { it.photoImageId })
            .containsExactlyInAnyOrder(existingOther.id, photo1.id, photo2.id)

        // Then: source에 photo1, photo2 유지
        val sourceLinks = photoImageFolderRepository.findAllByFolderIdIn(listOf(sourceFolder.id!!))
        assertThat(sourceLinks).hasSize(2)
        assertThat(sourceLinks.map { it.photoImageId }).containsExactlyInAnyOrder(photo1.id, photo2.id)
    }

    // ===================
    // Helper Methods
    // ===================

    private fun givenAuthenticated(): RequestSpecification = RestAssured.given()
        .contentType(ContentType.JSON)
        .header("Authorization", "Bearer $accessToken")

    private fun getFolderLatestImageUrl(folderId: Long): String? {
        val response = givenAuthenticated()
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
