package com.neki.photo.application.usecase

import com.neki.common.domain.vo.Pagination
import com.neki.common.domain.vo.SortOrder
import com.neki.photo.application.GetFavoritePhotosUseCase
import com.neki.photo.client.MediaClient
import com.neki.photo.dto.PhotoImageQuery
import com.neki.photo.models.MediaMetadata
import com.neki.photo.repository.PhotoImageRepository
import com.neki.photo.service.PhotoService
import com.neki.testfixture.FakeTransactionRunner
import com.neki.testfixture.aPhotoImage
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

private fun favoritePhotoWithCreatedAt(id: Long, userId: Long = 1L, mediaId: Long) =
    aPhotoImage(id = id, userId = userId, mediaId = mediaId).also {
        it.createdAt = LocalDateTime.of(2026, 1, 1, 12, 0)
    }

class GetFavoritePhotosUseCaseTest {

    lateinit var photoImageRepository: PhotoImageRepository
    lateinit var mediaClient: MediaClient
    lateinit var useCase: GetFavoritePhotosUseCase

    @BeforeEach
    fun setUp() {
        photoImageRepository = mockk()
        mediaClient = mockk()
        useCase =
            GetFavoritePhotosUseCase(
                PhotoService(photoImageRepository),
                mediaClient,
                FakeTransactionRunner(),
            )
    }

    private fun makeQuery(page: Int = 0, size: Int = 10) = PhotoImageQuery.GetFavoritePhotos(
        userId = 1L,
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
    @DisplayName("즐겨찾기 사진 정상 조회 시 목록 반환")
    fun `즐겨찾기 사진 정상 조회 시 목록 반환`() {
        // Given
        val query = makeQuery(size = 10)
        val photo1 = favoritePhotoWithCreatedAt(id = 1L, mediaId = 10L)
        val photo2 = favoritePhotoWithCreatedAt(id = 2L, mediaId = 20L)

        every { photoImageRepository.listOwnedFavoritePhotos(1L, 0, 11, SortOrder.DESC) } returns
            listOf(photo1, photo2)
        every { photoImageRepository.countOwnedFavoritePhotos(1L) } returns 2L
        every { mediaClient.getMediaMetadata(1L, listOf(10L, 20L)) } returns
            listOf(makeMediaInfo(10L), makeMediaInfo(20L))

        // When
        val result = useCase.execute(query)

        // Then
        result.photos shouldHaveSize 2
        result.hasNext shouldBe false
        result.totalCount shouldBe 2L
        result.photos[0].favorite shouldBe true
        result.photos[1].favorite shouldBe true
    }

    @Test
    @DisplayName("size+1개 조회 시 hasNext=true 반환")
    fun `size+1개 조회 시 hasNext=true 반환`() {
        // Given
        val query = makeQuery(size = 2)
        val photos = (1L..3L).map { favoritePhotoWithCreatedAt(id = it, mediaId = it * 10) }

        every { photoImageRepository.listOwnedFavoritePhotos(1L, 0, 3, SortOrder.DESC) } returns photos
        every { photoImageRepository.countOwnedFavoritePhotos(1L) } returns 3L
        every { mediaClient.getMediaMetadata(1L, listOf(10L, 20L)) } returns
            listOf(makeMediaInfo(10L), makeMediaInfo(20L))

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
        val photos = (1L..2L).map { favoritePhotoWithCreatedAt(id = it, mediaId = it * 10) }

        every { photoImageRepository.listOwnedFavoritePhotos(1L, 0, 3, SortOrder.DESC) } returns photos
        every { photoImageRepository.countOwnedFavoritePhotos(1L) } returns 2L
        every { mediaClient.getMediaMetadata(1L, listOf(10L, 20L)) } returns
            listOf(makeMediaInfo(10L), makeMediaInfo(20L))

        // When
        val result = useCase.execute(query)

        // Then
        result.hasNext shouldBe false
        result.photos shouldHaveSize 2
        result.totalCount shouldBe 2L
    }

    @Test
    @DisplayName("즐겨찾기 사진이 없는 경우 빈 결과와 hasNext=false 반환")
    fun `즐겨찾기 사진이 없는 경우 빈 결과와 hasNext=false 반환`() {
        // Given
        val query = makeQuery()

        every { photoImageRepository.listOwnedFavoritePhotos(1L, 0, 11, SortOrder.DESC) } returns emptyList()
        every { photoImageRepository.countOwnedFavoritePhotos(1L) } returns 0L

        // When
        val result = useCase.execute(query)

        // Then
        result.photos.shouldBeEmpty()
        result.hasNext shouldBe false
        result.totalCount shouldBe 0L
    }

    @Test
    @DisplayName("모든 사진의 미디어가 없는 경우 빈 결과 반환")
    fun `모든 사진의 미디어가 없는 경우 빈 결과 반환`() {
        // Given
        val query = makeQuery(size = 10)
        val photo1 = favoritePhotoWithCreatedAt(id = 1L, mediaId = 10L)

        every { photoImageRepository.listOwnedFavoritePhotos(1L, 0, 11, SortOrder.DESC) } returns listOf(photo1)
        every { photoImageRepository.countOwnedFavoritePhotos(1L) } returns 1L
        every { mediaClient.getMediaMetadata(1L, listOf(10L)) } returns emptyList()

        // When
        val result = useCase.execute(query)

        // Then
        result.photos.shouldBeEmpty()
        result.hasNext shouldBe false
        result.totalCount shouldBe 1L
    }
}
