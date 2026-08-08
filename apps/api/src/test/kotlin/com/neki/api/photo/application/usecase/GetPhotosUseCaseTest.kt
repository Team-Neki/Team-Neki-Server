package com.neki.api.photo.application.usecase

import com.neki.api.photo.application.GetPhotosUseCase
import com.neki.api.testfixture.aPhotoImage
import com.neki.core.domain.vo.Pagination
import com.neki.core.domain.vo.SortOrder
import com.neki.domain.photo.client.MediaClient
import com.neki.domain.photo.dto.PhotoImageQuery
import com.neki.domain.photo.models.MediaMetadata
import com.neki.domain.photo.models.PhotoWithFavorite
import com.neki.domain.photo.repository.PhotoImageRepository
import com.neki.domain.photo.service.PhotoService
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

private fun photoWithCreatedAt(id: Long, userId: Long = 1L, mediaId: Long) =
    aPhotoImage(id = id, userId = userId, mediaId = mediaId).also {
        it.createdAt = LocalDateTime.of(2026, 1, 1, 12, 0)
    }

class GetPhotosUseCaseTest {

    lateinit var photoImageRepository: PhotoImageRepository
    lateinit var mediaClient: MediaClient
    lateinit var useCase: GetPhotosUseCase

    @BeforeEach
    fun setUp() {
        photoImageRepository = mockk()
        mediaClient = mockk()
        useCase = GetPhotosUseCase(PhotoService(photoImageRepository), mediaClient)
    }

    private fun makeQuery(page: Int = 0, size: Int = 10, folderId: Long? = null) = PhotoImageQuery.GetPhotos(
        userId = 1L,
        folderId = folderId,
        pagination = Pagination(page = page, size = size, sortOrder = SortOrder.DESC),
    )

    private fun makeMediaInfo(mediaId: Long) = MediaMetadata(
        mediaId = mediaId,
        storageKey = "key/$mediaId.jpg",
        contentType = "image/jpeg",
        width = 800,
        height = 600,
    )

    @Test
    @DisplayName("정상 조회 시 사진 목록과 미디어 정보 매핑하여 반환")
    fun `정상 조회 시 사진 목록과 미디어 정보 매핑하여 반환`() {
        // Given
        val query = makeQuery(size = 10)
        val photo1 = photoWithCreatedAt(id = 1L, mediaId = 10L)
        val photo2 = photoWithCreatedAt(id = 2L, mediaId = 20L)
        val photosWithFavorite = listOf(
            PhotoWithFavorite(photo1, isFavorite = false),
            PhotoWithFavorite(photo2, isFavorite = true),
        )

        every { photoImageRepository.listOwnedPhotosWithFavorite(1L, null, 0, 11, SortOrder.DESC) } returns
            photosWithFavorite
        every { photoImageRepository.countOwnedPhotos(1L, null) } returns 2L
        every { mediaClient.getMediaMetadata(1L, listOf(10L, 20L)) } returns listOf(
            makeMediaInfo(10L),
            makeMediaInfo(20L),
        )

        // When
        val result = useCase.execute(query)

        // Then
        result.photos shouldHaveSize 2
        result.hasNext shouldBe false
        result.totalCount shouldBe 2L
        result.photos[0].photoId shouldBe 1L
        result.photos[0].storageKey shouldBe "key/10.jpg"
        result.photos[0].favorite shouldBe false
        result.photos[1].photoId shouldBe 2L
        result.photos[1].favorite shouldBe true
    }

    @Test
    @DisplayName("size+1개 조회 시 hasNext=true 반환")
    fun `size+1개 조회 시 hasNext=true 반환`() {
        // Given
        val query = makeQuery(size = 2)
        val photos = (1L..3L).map {
            PhotoWithFavorite(photoWithCreatedAt(id = it, mediaId = it * 10), false)
        }

        every { photoImageRepository.listOwnedPhotosWithFavorite(1L, null, 0, 3, SortOrder.DESC) } returns photos
        every { photoImageRepository.countOwnedPhotos(1L, null) } returns 3L
        every { mediaClient.getMediaMetadata(1L, listOf(10L, 20L)) } returns listOf(
            makeMediaInfo(10L),
            makeMediaInfo(20L),
        )

        // When
        val result = useCase.execute(query)

        // Then
        result.hasNext shouldBe true
        result.photos shouldHaveSize 2
        result.totalCount shouldBe 3L
    }

    @Test
    @DisplayName("정확히 size개 조회 시 hasNext=false 반환")
    fun `정확히 size개 조회 시 hasNext=false 반환`() {
        // Given
        val query = makeQuery(size = 2)
        val photos = (1L..2L).map {
            PhotoWithFavorite(photoWithCreatedAt(id = it, mediaId = it * 10), false)
        }

        every { photoImageRepository.listOwnedPhotosWithFavorite(1L, null, 0, 3, SortOrder.DESC) } returns photos
        every { photoImageRepository.countOwnedPhotos(1L, null) } returns 2L
        every { mediaClient.getMediaMetadata(1L, listOf(10L, 20L)) } returns listOf(
            makeMediaInfo(10L),
            makeMediaInfo(20L),
        )

        // When
        val result = useCase.execute(query)

        // Then
        result.hasNext shouldBe false
        result.photos shouldHaveSize 2
        result.totalCount shouldBe 2L
    }

    @Test
    @DisplayName("사진이 없는 경우 빈 결과와 hasNext=false 반환")
    fun `사진이 없는 경우 빈 결과와 hasNext=false 반환`() {
        // Given
        val query = makeQuery()

        every { photoImageRepository.listOwnedPhotosWithFavorite(1L, null, 0, 11, SortOrder.DESC) } returns
            emptyList()
        every { photoImageRepository.countOwnedPhotos(1L, null) } returns 0L

        // When
        val result = useCase.execute(query)

        // Then
        result.photos.shouldBeEmpty()
        result.hasNext shouldBe false
        result.totalCount shouldBe 0L
    }

    @Test
    @DisplayName("일부 미디어가 없는 경우 해당 사진은 결과에서 제외 (eventual consistency)")
    fun `일부 미디어가 없는 경우 해당 사진은 결과에서 제외 (eventual consistency)`() {
        // Given
        val query = makeQuery(size = 10)
        val photo1 = photoWithCreatedAt(id = 1L, mediaId = 10L)
        val photo2 = photoWithCreatedAt(id = 2L, mediaId = 20L)
        val photosWithFavorite = listOf(
            PhotoWithFavorite(photo1, isFavorite = false),
            PhotoWithFavorite(photo2, isFavorite = false),
        )

        every { photoImageRepository.listOwnedPhotosWithFavorite(1L, null, 0, 11, SortOrder.DESC) } returns
            photosWithFavorite
        every { photoImageRepository.countOwnedPhotos(1L, null) } returns 2L
        // mediaId=20L은 미존재
        every { mediaClient.getMediaMetadata(1L, listOf(10L, 20L)) } returns listOf(makeMediaInfo(10L))

        // When
        val result = useCase.execute(query)

        // Then
        result.photos shouldHaveSize 1
        result.photos[0].photoId shouldBe 1L
        result.totalCount shouldBe 2L
    }

    @Test
    @DisplayName("모든 사진의 미디어가 없는 경우 빈 결과 반환")
    fun `모든 사진의 미디어가 없는 경우 빈 결과 반환`() {
        // Given
        val query = makeQuery(size = 10)
        val photo1 = photoWithCreatedAt(id = 1L, mediaId = 10L)
        val photosWithFavorite = listOf(PhotoWithFavorite(photo1, isFavorite = false))

        every { photoImageRepository.listOwnedPhotosWithFavorite(1L, null, 0, 11, SortOrder.DESC) } returns
            photosWithFavorite
        every { photoImageRepository.countOwnedPhotos(1L, null) } returns 1L
        every { mediaClient.getMediaMetadata(1L, listOf(10L)) } returns emptyList()

        // When
        val result = useCase.execute(query)

        // Then
        result.photos.shouldBeEmpty()
        result.hasNext shouldBe false
        result.totalCount shouldBe 1L
    }
}
