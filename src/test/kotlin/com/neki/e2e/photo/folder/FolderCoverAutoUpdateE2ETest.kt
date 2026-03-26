package com.neki.e2e.photo.folder

import com.neki.common.api.dto.ResultCode
import com.neki.e2e.photo.image.PhotoImageE2ETestBase
import com.neki.media.api.dto.UploadTicketRequest
import com.neki.media.domain.MediaType
import com.neki.photo.api.dto.DeletePhotosRequest
import com.neki.photo.api.dto.RemovePhotosFromFolderRequest
import com.neki.photo.api.dto.UploadPhotoRequest
import com.neki.photo.domain.entity.PhotoImageFolder
import com.neki.photo.domain.enums.UploadMethod
import com.neki.user.domain.entity.User
import io.restassured.RestAssured
import io.restassured.http.ContentType
import org.assertj.core.api.Assertions.assertThat
import org.hamcrest.Matchers.equalTo
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.http.HttpStatus
import org.springframework.test.context.ActiveProfiles

/**
 * fileName       : FolderCoverAutoUpdateE2ETest
 * author         : claude
 * date           : 2026. 1. 28.
 * description    : 폴더 커버 이미지 조회 시점 파생 E2E 테스트
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class FolderCoverAutoUpdateE2ETest : PhotoImageE2ETestBase() {

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
    // Helper Methods
    // ===================

    private fun createMediaIds(count: Int): List<Long> {
        val ticketRequest = UploadTicketRequest(
            items = (1..count).map {
                UploadTicketRequest.UploadTicketItem(
                    filename = "photo$it.jpg",
                    contentType = "image/jpeg",
                    mediaType = MediaType.PHOTO_BOOTH,
                )
            },
        )

        return RestAssured.given()
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer $accessToken")
            .body(ticketRequest)
            .`when`()
            .post("/api/media/upload")
            .then()
            .statusCode(HttpStatus.OK.value())
            .extract()
            .jsonPath()
            .getList<Int>("data.items.mediaId")
            .map { it.toLong() }
    }

    private fun uploadPhotosViaApi(folderId: Long?, mediaIds: List<Long>) {
        val uploadRequest = UploadPhotoRequest(
            folderId = folderId,
            uploads = mediaIds.map {
                UploadPhotoRequest.UploadPhotoItem(
                    mediaId = it,
                    uploadMethod = UploadMethod.DIRECT_UPLOAD,
                    memo = null,
                    capturedAt = null,
                )
            },
        )

        RestAssured.given()
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer $accessToken")
            .body(uploadRequest)
            .`when`()
            .post("/api/photos")
            .then()
            .statusCode(HttpStatus.OK.value())
            .body("resultCode", equalTo(ResultCode.SUCCESS.code))
    }

    private fun deletePhotosViaApi(photoIds: List<Long>) {
        RestAssured.given()
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer $accessToken")
            .body(DeletePhotosRequest(photoIds = photoIds))
            .`when`()
            .delete("/api/photos")
            .then()
            .statusCode(HttpStatus.OK.value())
            .body("resultCode", equalTo(ResultCode.SUCCESS.code))
    }

    private fun getFolderInfo(folderId: Long): FolderInfoDto {
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

        return FolderInfoDto(
            folderId = (folderData["folderId"] as Number).toLong(),
            name = folderData["name"] as String,
            latestImageUrl = folderData["latestImageUrl"] as String?,
            totalCount = (folderData["totalCount"] as Number).toLong(),
        )
    }

    private fun getPhotoIdsInFolder(folderId: Long?): List<Long> {
        if (folderId == null) return emptyList()
        val photoImageIds: List<Long> = photoImageFolderRepository.findAllByFolderIdIn(listOf(folderId))
            .map { it.photoImageId }
        return photoImageIds.sortedDescending()
    }

    data class FolderInfoDto(val folderId: Long, val name: String, val latestImageUrl: String?, val totalCount: Long)

    // ===================
    // Test Cases
    // ===================

    @Test
    @DisplayName("사진 업로드 후 폴더 조회 시 커버가 최신 사진으로 파생됨")
    fun givenEmptyFolder_whenUploadPhotosViaApi_thenCoverIsSetToLatestPhoto() {
        // Given: 빈 폴더 생성
        val folder = createFolder(testUser.id!!, "테스트 폴더")
        val mediaIds = createMediaIds(3)

        // When: 사진 3장 업로드
        uploadPhotosViaApi(folder.id, mediaIds)

        // Then: 폴더의 커버가 설정되어야 함
        val folderInfo = getFolderInfo(folder.id!!)
        assertThat(folderInfo.latestImageUrl).isNotNull()
        assertThat(folderInfo.totalCount).isEqualTo(3)
    }

    @Test
    @DisplayName("추가 사진 업로드 후 조회 시 커버가 최신 사진으로 파생됨")
    fun givenFolderWithPhotos_whenUploadMorePhotos_thenCoverUpdatesToNewerPhoto() {
        // Given: 폴더에 사진 2장 업로드
        val folder = createFolder(testUser.id!!, "테스트 폴더")
        val initialMediaIds = createMediaIds(2)
        uploadPhotosViaApi(folder.id, initialMediaIds)

        val initialFolderInfo = getFolderInfo(folder.id!!)
        val initialCoverUrl = initialFolderInfo.latestImageUrl

        // When: 추가로 사진 2장 업로드
        val additionalMediaIds = createMediaIds(2)
        uploadPhotosViaApi(folder.id, additionalMediaIds)

        // Then: 커버 URL이 변경되고, 총 개수가 4개
        val updatedFolderInfo = getFolderInfo(folder.id!!)
        assertThat(updatedFolderInfo.latestImageUrl).isNotNull()
        assertThat(updatedFolderInfo.latestImageUrl).isNotEqualTo(initialCoverUrl)
        assertThat(updatedFolderInfo.totalCount).isEqualTo(4)
    }

    @Test
    @DisplayName("폴더 미지정 시 폴더 커버는 변경되지 않음")
    fun givenNoFolderId_whenUploadPhotos_thenNoCoverUpdated() {
        // Given: 빈 폴더 생성 (사진 없음)
        val folder = createFolder(testUser.id!!, "빈 폴더")
        val mediaIds = createMediaIds(2)

        // When: folderId = null로 사진 업로드
        uploadPhotosViaApi(null, mediaIds)

        // Then: 폴더의 커버는 null 유지, totalCount는 0
        val folderInfo = getFolderInfo(folder.id!!)
        assertThat(folderInfo.latestImageUrl).isNull()
        assertThat(folderInfo.totalCount).isEqualTo(0)
    }

    @Test
    @DisplayName("최신 사진 삭제 후 조회 시 커버가 다음 최신 사진으로 파생됨")
    fun givenFolderWithCoverPhoto_whenDeleteCoverPhotoViaApi_thenCoverUpdatesToNextLatest() {
        // Given: 폴더에 사진 3장 업로드
        val folder = createFolder(testUser.id!!, "테스트 폴더")
        val mediaIds = createMediaIds(3)
        uploadPhotosViaApi(folder.id, mediaIds)

        val initialFolderInfo = getFolderInfo(folder.id!!)
        val initialCoverUrl = initialFolderInfo.latestImageUrl

        // 커버 사진 ID 확인 (가장 최근 업로드된 사진 = 첫 번째)
        val photoIds = getPhotoIdsInFolder(folder.id)
        val coverPhotoId = photoIds[0] // DESC 정렬이므로 첫 번째가 가장 최근

        // When: 커버 사진 삭제
        deletePhotosViaApi(listOf(coverPhotoId))

        // Then: 커버 URL이 변경되고, totalCount가 2
        val updatedFolderInfo = getFolderInfo(folder.id!!)
        assertThat(updatedFolderInfo.latestImageUrl).isNotNull()
        assertThat(updatedFolderInfo.latestImageUrl).isNotEqualTo(initialCoverUrl)
        assertThat(updatedFolderInfo.totalCount).isEqualTo(2)
    }

    @Test
    @DisplayName("모든 사진 삭제 후 조회 시 커버가 null로 파생됨")
    fun givenFolderWithPhotos_whenDeleteAllPhotosViaApi_thenCoverBecomesNull() {
        // Given: 폴더에 사진 2장 업로드
        val folder = createFolder(testUser.id!!, "테스트 폴더")
        val mediaIds = createMediaIds(2)
        uploadPhotosViaApi(folder.id, mediaIds)

        // 모든 사진 ID 조회
        val photoIds = getPhotoIdsInFolder(folder.id)
        assertThat(photoIds).hasSize(2)

        // When: 모든 사진 삭제
        deletePhotosViaApi(photoIds)

        // Then: 커버가 null, totalCount가 0
        val updatedFolderInfo = getFolderInfo(folder.id!!)
        assertThat(updatedFolderInfo.latestImageUrl).isNull()
        assertThat(updatedFolderInfo.totalCount).isEqualTo(0)
    }

    @Test
    @DisplayName("여러 폴더의 최신 사진 일괄 삭제 후 조회 시 각 폴더 커버가 파생됨")
    fun givenMultipleFolders_whenBatchDeleteCoverPhotos_thenAllCoversUpdateToNextLatest() {
        // Given: 폴더 A, B, C 각각 생성
        val folderA = createFolder(testUser.id!!, "폴더 A")
        val folderB = createFolder(testUser.id!!, "폴더 B")
        val folderC = createFolder(testUser.id!!, "폴더 C")

        // 각 폴더에 사진 3장씩 업로드
        uploadPhotosViaApi(folderA.id, createMediaIds(3))
        uploadPhotosViaApi(folderB.id, createMediaIds(3))
        uploadPhotosViaApi(folderC.id, createMediaIds(3))

        // 각 폴더의 초기 커버 URL 기록
        val initialCoverA = getFolderInfo(folderA.id!!).latestImageUrl
        val initialCoverB = getFolderInfo(folderB.id!!).latestImageUrl
        val initialCoverC = getFolderInfo(folderC.id!!).latestImageUrl

        // 각 폴더의 커버 사진 ID 확인 (가장 최근에 업로드된 사진 = 마지막 업로드된 사진)
        val coverPhotoIdA = getPhotoIdsInFolder(folderA.id)[0]
        val coverPhotoIdB = getPhotoIdsInFolder(folderB.id)[0]
        val coverPhotoIdC = getPhotoIdsInFolder(folderC.id)[0]

        // When: 3개 커버 사진을 한 번에 삭제
        deletePhotosViaApi(listOf(coverPhotoIdA, coverPhotoIdB, coverPhotoIdC))

        // Then: 모든 폴더의 커버가 업데이트됨
        val updatedInfoA = getFolderInfo(folderA.id!!)
        val updatedInfoB = getFolderInfo(folderB.id!!)
        val updatedInfoC = getFolderInfo(folderC.id!!)

        // 모든 폴더의 커버 URL이 변경됨
        assertThat(updatedInfoA.latestImageUrl).isNotEqualTo(initialCoverA)
        assertThat(updatedInfoB.latestImageUrl).isNotEqualTo(initialCoverB)
        assertThat(updatedInfoC.latestImageUrl).isNotEqualTo(initialCoverC)

        // 모든 폴더의 totalCount가 2
        assertThat(updatedInfoA.totalCount).isEqualTo(2)
        assertThat(updatedInfoB.totalCount).isEqualTo(2)
        assertThat(updatedInfoC.totalCount).isEqualTo(2)

        // 모든 폴더의 커버가 non-null (아직 사진이 남아있으므로)
        assertThat(updatedInfoA.latestImageUrl).isNotNull()
        assertThat(updatedInfoB.latestImageUrl).isNotNull()
        assertThat(updatedInfoC.latestImageUrl).isNotNull()
    }

    @Test
    @DisplayName("혼합 삭제 후 조회 시 각 폴더 커버가 상태에 맞게 파생됨")
    fun givenMixedFolderStates_whenBatchDeleteCovers_thenEachFolderUpdatesAppropriately() {
        // Given:
        // 폴더 A: 사진 1장 (삭제 후 비워짐)
        // 폴더 B: 사진 3장 (삭제 후 2장 남음)
        val folderA = createFolder(testUser.id!!, "폴더 A")
        val folderB = createFolder(testUser.id!!, "폴더 B")

        uploadPhotosViaApi(folderA.id, createMediaIds(1))

        // 폴더 B에 사진 3장 업로드
        uploadPhotosViaApi(folderB.id, createMediaIds(3))

        // 각 폴더의 커버 사진 ID 확인 (가장 최근 업로드된 사진)
        val coverPhotoIdA = getPhotoIdsInFolder(folderA.id)[0]
        val coverPhotoIdB = getPhotoIdsInFolder(folderB.id)[0]

        // When: 두 폴더의 커버 사진 일괄 삭제
        deletePhotosViaApi(listOf(coverPhotoIdA, coverPhotoIdB))

        // Then:
        // 폴더 A: latestImageUrl = null, totalCount = 0
        val updatedInfoA = getFolderInfo(folderA.id!!)
        assertThat(updatedInfoA.totalCount).isEqualTo(0)
        assertThat(updatedInfoA.latestImageUrl).isNull()

        // 폴더 B: latestImageUrl non-null, totalCount = 2
        val updatedInfoB = getFolderInfo(folderB.id!!)
        assertThat(updatedInfoB.latestImageUrl).isNotNull()
        assertThat(updatedInfoB.totalCount).isEqualTo(2)
    }

    @Test
    @DisplayName("비최신 사진 삭제 후 조회 시 커버는 동일하게 파생됨")
    fun givenFolderWithCoverPhoto_whenDeleteNonCoverPhoto_thenCoverRemainsUnchanged() {
        // Given: 폴더에 사진 3장 업로드
        val folder = createFolder(testUser.id!!, "테스트 폴더")
        uploadPhotosViaApi(folder.id, createMediaIds(3))

        val initialFolderInfo = getFolderInfo(folder.id!!)
        val initialCoverUrl = initialFolderInfo.latestImageUrl

        // 가장 오래된 사진 ID 확인 (DESC 정렬이므로 마지막)
        val photoIds = getPhotoIdsInFolder(folder.id)
        val oldestPhotoId = photoIds.last()

        // When: 가장 오래된 사진(비커버) 삭제
        deletePhotosViaApi(listOf(oldestPhotoId))

        // Then: 커버 URL은 동일, totalCount가 2
        val updatedFolderInfo = getFolderInfo(folder.id!!)
        assertThat(updatedFolderInfo.latestImageUrl).isEqualTo(initialCoverUrl)
        assertThat(updatedFolderInfo.totalCount).isEqualTo(2)
    }

    @Test
    @DisplayName("여러 폴더에 속한 사진을 한 폴더에서 제외해도 다른 폴더의 stats는 영향받지 않는다")
    fun givenPhotoInMultipleFolders_whenDeleteFromOneFolder_thenOtherFolderStatsCorrect() {
        // Given: 폴더 A, B 생성
        val folderA = createFolder(testUser.id!!, "폴더 A")
        val folderB = createFolder(testUser.id!!, "폴더 B")

        // 폴더 A에 사진 2장 업로드
        val mediaIdsA = createMediaIds(2)
        uploadPhotosViaApi(folderA.id, mediaIdsA)

        // 폴더 A의 사진 중 1장을 폴더 B에도 연결
        val photoIdsInA = getPhotoIdsInFolder(folderA.id)
        val sharedPhotoId = photoIdsInA[0]
        photoImageFolderRepository.save(PhotoImageFolder(photoImageId = sharedPhotoId, folderId = folderB.id!!))

        // 폴더 B의 초기 stats 확인
        val initialInfoB = getFolderInfo(folderB.id!!)
        assertThat(initialInfoB.totalCount).isEqualTo(1)
        assertThat(initialInfoB.latestImageUrl).isNotNull()

        // When: 폴더 A에서 공유 사진 제외
        RestAssured.given()
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer $accessToken")
            .body(RemovePhotosFromFolderRequest(photoIds = listOf(sharedPhotoId)))
            .`when`()
            .delete("/api/folders/${folderA.id}/photos")
            .then()
            .statusCode(HttpStatus.OK.value())
            .body("resultCode", equalTo(ResultCode.SUCCESS.code))

        // Then: 폴더 A의 count가 1로 감소
        val updatedInfoA = getFolderInfo(folderA.id!!)
        assertThat(updatedInfoA.totalCount).isEqualTo(1)

        // 폴더 B의 stats는 변경되지 않음
        val updatedInfoB = getFolderInfo(folderB.id!!)
        assertThat(updatedInfoB.totalCount).isEqualTo(1)
        assertThat(updatedInfoB.latestImageUrl).isNotNull()
    }
}
