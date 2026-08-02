package com.neki.photo.application.usecase

import com.neki.common.code.ResultCode
import com.neki.common.exception.BusinessException
import com.neki.photo.application.dto.FolderCommand
import com.neki.photo.application.port.FolderRepositoryPort
import com.neki.testfixture.aFolder
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

class UpdateFolderUseCaseTest {

    lateinit var folderRepository: FolderRepositoryPort
    lateinit var useCase: UpdateFolderUseCase

    @BeforeEach
    fun setUp() {
        folderRepository = mockk()
        useCase = UpdateFolderUseCase(folderRepository)
    }

    @Test
    @DisplayName("폴더가 존재하고 새 이름이 고유한 경우 이름 수정")
    fun `폴더가 존재하고 새 이름이 고유한 경우 이름 수정`() {
        // Given
        val folder = aFolder(id = 1L, userId = 1L, name = "기존 폴더")
        val command = FolderCommand.UpdateFolder(userId = 1L, folderId = 1L, newName = "새 이름")

        every { folderRepository.getOwnedFolder(1L, 1L) } returns folder
        every { folderRepository.existsOwnedFolderName(1L, "새 이름") } returns false

        // When
        useCase.execute(command)

        // Then
        folder.name shouldBe "새 이름"
    }

    @Test
    @DisplayName("폴더가 존재하지 않는 경우 NOT_FOUND 예외 발생")
    fun `폴더가 존재하지 않는 경우 NOT_FOUND 예외 발생`() {
        // Given
        val command = FolderCommand.UpdateFolder(userId = 1L, folderId = 99L, newName = "새 이름")

        every { folderRepository.getOwnedFolder(1L, 99L) } returns null

        // When & Then
        val ex = shouldThrow<BusinessException> {
            useCase.execute(command)
        }
        ex.resultCode shouldBe ResultCode.NOT_FOUND
    }

    @Test
    @DisplayName("현재 이름과 동일한 경우 충돌 검사 skip 후 정상 처리")
    fun `현재 이름과 동일한 경우 충돌 검사 skip 후 정상 처리`() {
        // Given
        val folder = aFolder(id = 1L, userId = 1L, name = "기존 폴더")
        val command = FolderCommand.UpdateFolder(userId = 1L, folderId = 1L, newName = "기존 폴더")

        every { folderRepository.getOwnedFolder(1L, 1L) } returns folder

        // When
        useCase.execute(command)

        // Then
        verify(exactly = 0) { folderRepository.existsOwnedFolderName(any(), any()) }
        folder.name shouldBe "기존 폴더"
    }

    @Test
    @DisplayName("새 이름이 이미 존재하는 경우 CONFLICT_FOLDER 예외 발생")
    fun `새 이름이 이미 존재하는 경우 CONFLICT_FOLDER 예외 발생`() {
        // Given
        val folder = aFolder(id = 1L, userId = 1L, name = "기존 폴더")
        val command = FolderCommand.UpdateFolder(userId = 1L, folderId = 1L, newName = "중복 이름")

        every { folderRepository.getOwnedFolder(1L, 1L) } returns folder
        every { folderRepository.existsOwnedFolderName(1L, "중복 이름") } returns true

        // When & Then
        val ex = shouldThrow<BusinessException> {
            useCase.execute(command)
        }
        ex.resultCode shouldBe ResultCode.CONFLICT_FOLDER
    }
}
