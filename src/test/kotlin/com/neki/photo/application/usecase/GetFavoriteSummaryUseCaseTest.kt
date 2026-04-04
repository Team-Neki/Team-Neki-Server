package com.neki.photo.application.usecase

import com.neki.photo.application.command.GetFavoriteSummaryCommand
import com.neki.photo.application.contract.MediaStorageInfo
import com.neki.photo.application.port.FavoriteImageRepositoryPort
import com.neki.photo.application.port.MediaClientPort
import com.neki.photo.application.port.PhotoImageRepositoryPort
import com.neki.testfixture.FakeTransactionRunner
import com.neki.testfixture.aPhotoImage
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk

class GetFavoriteSummaryUseCaseTest : FunSpec({

    lateinit var favoriteImageRepository: FavoriteImageRepositoryPort
    lateinit var photoImageRepository: PhotoImageRepositoryPort
    lateinit var mediaClient: MediaClientPort
    lateinit var useCase: GetFavoriteSummaryUseCase

    beforeTest {
        favoriteImageRepository = mockk()
        photoImageRepository = mockk()
        mediaClient = mockk()
        useCase = GetFavoriteSummaryUseCase(
            favoriteImageRepository,
            photoImageRepository,
            mediaClient,
            FakeTransactionRunner(),
        )
    }

    val command = GetFavoriteSummaryCommand(userId = 1L)

    test("즐겨찾기 count=0이면 storageKey=null, totalCount=0 반환") {
        // Given
        every { favoriteImageRepository.countByUserId(1L) } returns 0L

        // When
        val result = useCase.execute(command)

        // Then
        result.storageKey shouldBe null
        result.totalCount shouldBe 0L
    }

    test("count>0이지만 최신 photo가 없는 경우 storageKey=null 반환") {
        // Given
        every { favoriteImageRepository.countByUserId(1L) } returns 3L
        every { photoImageRepository.getLatestFavoritePhoto(1L) } returns null

        // When
        val result = useCase.execute(command)

        // Then
        result.storageKey shouldBe null
        result.totalCount shouldBe 3L
    }

    test("count>0이고 photo 있으나 미디어가 없는 경우 storageKey=null 반환") {
        // Given
        val photo = aPhotoImage(id = 1L, userId = 1L, mediaId = 10L)

        every { favoriteImageRepository.countByUserId(1L) } returns 3L
        every { photoImageRepository.getLatestFavoritePhoto(1L) } returns photo
        every { mediaClient.getMediaStorageInfos(1L, listOf(10L)) } returns emptyList()

        // When
        val result = useCase.execute(command)

        // Then
        result.storageKey shouldBe null
        result.totalCount shouldBe 3L
    }

    test("count>0이고 photo와 미디어가 모두 존재하는 경우 count와 storageKey 반환") {
        // Given
        val photo = aPhotoImage(id = 1L, userId = 1L, mediaId = 10L)
        val mediaInfo = MediaStorageInfo(
            mediaId = 10L,
            storageKey = "key/cover.jpg",
            contentType = "image/jpeg",
        )

        every { favoriteImageRepository.countByUserId(1L) } returns 5L
        every { photoImageRepository.getLatestFavoritePhoto(1L) } returns photo
        every { mediaClient.getMediaStorageInfos(1L, listOf(10L)) } returns listOf(mediaInfo)

        // When
        val result = useCase.execute(command)

        // Then
        result.storageKey shouldBe "key/cover.jpg"
        result.totalCount shouldBe 5L
    }

    test("count>0이지만 latestPhoto가 null인 경우 - 즐겨찾기 수와 실제 photo 간 불일치 처리") {
        // Given
        every { favoriteImageRepository.countByUserId(1L) } returns 2L
        every { photoImageRepository.getLatestFavoritePhoto(1L) } returns null

        // When
        val result = useCase.execute(command)

        // Then
        result.storageKey shouldBe null
        result.totalCount shouldBe 2L
    }
})
