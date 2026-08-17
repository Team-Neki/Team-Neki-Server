package com.neki.api.photo.application.usecase

import com.neki.api.photo.application.GetFoldersUseCase
import com.neki.domain.photo.client.MediaClient
import com.neki.domain.photo.dto.FolderQuery
import com.neki.domain.photo.models.FolderStats
import com.neki.domain.photo.models.MediaMetadata
import com.neki.domain.photo.repository.FolderRepository
import com.neki.domain.photo.service.FolderService
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

class GetFoldersUseCaseTest {

    lateinit var folderRepository: FolderRepository
    lateinit var mediaClient: MediaClient
    lateinit var useCase: GetFoldersUseCase

    @BeforeEach
    fun setUp() {
        folderRepository = mockk()
        mediaClient = mockk()
        useCase = GetFoldersUseCase(FolderService(folderRepository, mockk()), mediaClient)
    }

    @Test
    @DisplayName("폴더 목록 정상 조회 시 FolderInfo 목록 반환")
    fun `폴더 목록 정상 조회 시 FolderInfo 목록 반환`() {
        // Given
        val query = FolderQuery.GetFolders(userId = 1L, limit = 10)
        val foldersWithStats = listOf(
            FolderStats(folderId = 1L, name = "폴더1", coverMediaId = 10L, photoCount = 5L),
            FolderStats(folderId = 2L, name = "폴더2", coverMediaId = 20L, photoCount = 3L),
        )

        every { folderRepository.listOwnedFoldersWithStats(1L, 10) } returns foldersWithStats
        every { mediaClient.getMediaMetadata(1L, listOf(10L, 20L)) } returns listOf(
            MediaMetadata(mediaId = 10L, storageKey = "key/image1.jpg", contentType = "image/jpeg"),
            MediaMetadata(mediaId = 20L, storageKey = "key/image2.jpg", contentType = "image/jpeg"),
        )

        // When
        val result = useCase.execute(query)

        // Then
        result.items shouldHaveSize 2
        result.items[0].folderId shouldBe 1L
        result.items[0].name shouldBe "폴더1"
        result.items[0].storageKey shouldBe "key/image1.jpg"
        result.items[0].count shouldBe 5L
    }

    @Test
    @DisplayName("폴더가 없는 경우 빈 목록 반환")
    fun `폴더가 없는 경우 빈 목록 반환`() {
        // Given
        val query = FolderQuery.GetFolders(userId = 1L, limit = null)

        every { folderRepository.listOwnedFoldersWithStats(1L, null) } returns emptyList()

        // When
        val result = useCase.execute(query)

        // Then
        result.items.shouldBeEmpty()
    }

    @Test
    @DisplayName("coverMediaId가 null인 폴더는 storageKey에 null 반환")
    fun `coverMediaId가 null인 폴더는 storageKey에 null 반환`() {
        // Given
        val query = FolderQuery.GetFolders(userId = 1L, limit = null)
        val foldersWithStats = listOf(
            FolderStats(folderId = 1L, name = "빈 폴더", coverMediaId = null, photoCount = 0L),
        )

        every { folderRepository.listOwnedFoldersWithStats(1L, null) } returns foldersWithStats

        // When
        val result = useCase.execute(query)

        // Then
        result.items shouldHaveSize 1
        result.items[0].storageKey shouldBe null
    }
}
