package com.neki.photo.application.usecase

import com.neki.common.domain.vo.SortOrder
import com.neki.photo.application.command.GetPhotosCommand
import com.neki.photo.application.contract.MediaStorageInfo
import com.neki.photo.application.contract.PhotoWithFavorite
import com.neki.photo.application.port.MediaClientPort
import com.neki.photo.application.port.PhotoImageRepositoryPort
import com.neki.testfixture.aPhotoImage
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import java.time.LocalDateTime

private fun photoWithCreatedAt(id: Long, userId: Long = 1L, mediaId: Long) =
    aPhotoImage(id = id, userId = userId, mediaId = mediaId).also {
        it.createdAt = LocalDateTime.of(2026, 1, 1, 12, 0)
    }

class GetPhotosUseCaseTest : FunSpec({

    lateinit var photoImageRepository: PhotoImageRepositoryPort
    lateinit var mediaClient: MediaClientPort
    lateinit var useCase: GetPhotosUseCase

    beforeTest {
        photoImageRepository = mockk()
        mediaClient = mockk()
        useCase = GetPhotosUseCase(photoImageRepository, mediaClient)
    }

    fun makeCommand(page: Int = 0, size: Int = 10, folderId: Long? = null) =
        GetPhotosCommand(userId = 1L, folderId = folderId, page = page, size = size, sortOrder = SortOrder.DESC)

    fun makeMediaInfo(mediaId: Long) = MediaStorageInfo(
        mediaId = mediaId,
        storageKey = "key/$mediaId.jpg",
        contentType = "image/jpeg",
        width = 800,
        height = 600,
    )

    test("정상 조회 시 사진 목록과 미디어 정보 매핑하여 반환") {
        // Given
        val command = makeCommand(size = 10)
        val photo1 = photoWithCreatedAt(id = 1L, mediaId = 10L)
        val photo2 = photoWithCreatedAt(id = 2L, mediaId = 20L)
        val photosWithFavorite = listOf(
            PhotoWithFavorite(photo1, isFavorite = false),
            PhotoWithFavorite(photo2, isFavorite = true),
        )

        every { photoImageRepository.listOwnedPhotosWithFavorite(1L, null, 0, 11, SortOrder.DESC) } returns photosWithFavorite
        every { mediaClient.getMediaStorageInfos(1L, listOf(10L, 20L)) } returns listOf(
            makeMediaInfo(10L),
            makeMediaInfo(20L),
        )

        // When
        val result = useCase.execute(command)

        // Then
        result.photos shouldHaveSize 2
        result.hasNext shouldBe false
        result.photos[0].photoId shouldBe 1L
        result.photos[0].storageKey shouldBe "key/10.jpg"
        result.photos[0].favorite shouldBe false
        result.photos[1].photoId shouldBe 2L
        result.photos[1].favorite shouldBe true
    }

    test("size+1개 조회 시 hasNext=true 반환") {
        // Given
        val command = makeCommand(size = 2)
        val photos = (1L..3L).map { PhotoWithFavorite(photoWithCreatedAt(id = it, mediaId = it * 10), false) }

        every { photoImageRepository.listOwnedPhotosWithFavorite(1L, null, 0, 3, SortOrder.DESC) } returns photos
        every { mediaClient.getMediaStorageInfos(1L, listOf(10L, 20L)) } returns listOf(
            makeMediaInfo(10L),
            makeMediaInfo(20L),
        )

        // When
        val result = useCase.execute(command)

        // Then
        result.hasNext shouldBe true
        result.photos shouldHaveSize 2
    }

    test("정확히 size개 조회 시 hasNext=false 반환") {
        // Given
        val command = makeCommand(size = 2)
        val photos = (1L..2L).map { PhotoWithFavorite(photoWithCreatedAt(id = it, mediaId = it * 10), false) }

        every { photoImageRepository.listOwnedPhotosWithFavorite(1L, null, 0, 3, SortOrder.DESC) } returns photos
        every { mediaClient.getMediaStorageInfos(1L, listOf(10L, 20L)) } returns listOf(
            makeMediaInfo(10L),
            makeMediaInfo(20L),
        )

        // When
        val result = useCase.execute(command)

        // Then
        result.hasNext shouldBe false
        result.photos shouldHaveSize 2
    }

    test("사진이 없는 경우 빈 결과와 hasNext=false 반환") {
        // Given
        val command = makeCommand()

        every { photoImageRepository.listOwnedPhotosWithFavorite(1L, null, 0, 11, SortOrder.DESC) } returns emptyList()

        // When
        val result = useCase.execute(command)

        // Then
        result.photos.shouldBeEmpty()
        result.hasNext shouldBe false
    }

    test("일부 미디어가 없는 경우 해당 사진은 결과에서 제외 (eventual consistency)") {
        // Given
        val command = makeCommand(size = 10)
        val photo1 = photoWithCreatedAt(id = 1L, mediaId = 10L)
        val photo2 = photoWithCreatedAt(id = 2L, mediaId = 20L)
        val photosWithFavorite = listOf(
            PhotoWithFavorite(photo1, isFavorite = false),
            PhotoWithFavorite(photo2, isFavorite = false),
        )

        every { photoImageRepository.listOwnedPhotosWithFavorite(1L, null, 0, 11, SortOrder.DESC) } returns photosWithFavorite
        // mediaId=20L은 미존재
        every { mediaClient.getMediaStorageInfos(1L, listOf(10L, 20L)) } returns listOf(makeMediaInfo(10L))

        // When
        val result = useCase.execute(command)

        // Then
        result.photos shouldHaveSize 1
        result.photos[0].photoId shouldBe 1L
    }

    test("모든 사진의 미디어가 없는 경우 빈 결과 반환") {
        // Given
        val command = makeCommand(size = 10)
        val photo1 = photoWithCreatedAt(id = 1L, mediaId = 10L)
        val photosWithFavorite = listOf(PhotoWithFavorite(photo1, isFavorite = false))

        every { photoImageRepository.listOwnedPhotosWithFavorite(1L, null, 0, 11, SortOrder.DESC) } returns photosWithFavorite
        every { mediaClient.getMediaStorageInfos(1L, listOf(10L)) } returns emptyList()

        // When
        val result = useCase.execute(command)

        // Then
        result.photos.shouldBeEmpty()
        result.hasNext shouldBe false
    }
})
