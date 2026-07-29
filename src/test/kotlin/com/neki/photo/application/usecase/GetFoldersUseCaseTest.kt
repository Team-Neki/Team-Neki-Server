package com.neki.photo.application.usecase

import com.neki.photo.application.dto.FolderQuery
import com.neki.photo.application.port.FolderRepositoryPort
import com.neki.photo.application.port.dto.PhotoContract
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

class GetFoldersUseCaseTest {

    lateinit var folderRepository: FolderRepositoryPort
    lateinit var useCase: GetFoldersUseCase

    @BeforeEach
    fun setUp() {
        folderRepository = mockk()
        useCase = GetFoldersUseCase(folderRepository)
    }

    @Test
    @DisplayName("폴더 목록 정상 조회 시 FolderInfo 목록 반환")
    fun `폴더 목록 정상 조회 시 FolderInfo 목록 반환`() {
        // Given
        val query = FolderQuery.GetFolders(userId = 1L, limit = 10)
        val foldersWithStats = listOf(
            PhotoContract.FolderWithStats(
                folderId = 1L,
                name = "폴더1",
                coverImageStorageKey = "key/image1.jpg",
                photoCount = 5L,
            ),
            PhotoContract.FolderWithStats(
                folderId = 2L,
                name = "폴더2",
                coverImageStorageKey = "key/image2.jpg",
                photoCount = 3L,
            ),
        )

        every { folderRepository.listOwnedFoldersWithStats(1L, 10) } returns foldersWithStats

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
    @DisplayName("coverImageStorageKey가 null인 폴더는 storageKey에 null 반환")
    fun `coverImageStorageKey가 null인 폴더는 storageKey에 null 반환`() {
        // Given
        val query = FolderQuery.GetFolders(userId = 1L, limit = null)
        val foldersWithStats = listOf(
            PhotoContract.FolderWithStats(folderId = 1L, name = "빈 폴더", coverImageStorageKey = null, photoCount = 0L),
        )

        every { folderRepository.listOwnedFoldersWithStats(1L, null) } returns foldersWithStats

        // When
        val result = useCase.execute(query)

        // Then
        result.items shouldHaveSize 1
        result.items[0].storageKey shouldBe null
    }
}
