package com.neki.photo.application.usecase

import com.neki.common.code.ResultCode
import com.neki.common.exception.BusinessException
import com.neki.photo.application.CreateFolderUseCase
import com.neki.photo.dto.FolderCommand
import com.neki.photo.repository.FolderRepository
import com.neki.photo.service.FolderService
import com.neki.testfixture.aFolder
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

class CreateFolderUseCaseTest {

    lateinit var folderRepository: FolderRepository
    lateinit var useCase: CreateFolderUseCase

    @BeforeEach
    fun setUp() {
        folderRepository = mockk()
        useCase = CreateFolderUseCase(FolderService(folderRepository, mockk()))
    }

    @Test
    @DisplayName("이름이 고유한 경우 폴더 저장 후 ID 반환")
    fun `이름이 고유한 경우 폴더 저장 후 ID 반환`() {
        // Given
        val command = FolderCommand.CreateFolder(userId = 1L, name = "새 폴더")
        val savedFolder = aFolder(id = 42L, userId = 1L, name = "새 폴더")

        every { folderRepository.existsOwnedFolderName(1L, "새 폴더") } returns false
        every { folderRepository.save(any()) } returns savedFolder

        // When
        val result = useCase.execute(command)

        // Then
        result.folderId shouldBe 42L
        verify(exactly = 1) { folderRepository.save(any()) }
    }

    @Test
    @DisplayName("이름이 중복된 경우 CONFLICT_FOLDER 예외 발생")
    fun `이름이 중복된 경우 CONFLICT_FOLDER 예외 발생`() {
        // Given
        val command = FolderCommand.CreateFolder(userId = 1L, name = "중복 폴더")

        every { folderRepository.existsOwnedFolderName(1L, "중복 폴더") } returns true

        // When & Then
        val ex = shouldThrow<BusinessException> {
            useCase.execute(command)
        }
        ex.resultCode shouldBe ResultCode.CONFLICT_FOLDER
        verify(exactly = 0) { folderRepository.save(any()) }
    }
}
