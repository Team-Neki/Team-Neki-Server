package com.neki.photo.application.usecase

import com.neki.common.api.dto.ResultCode
import com.neki.common.exception.BusinessException
import com.neki.photo.application.command.CreateFolderCommand
import com.neki.photo.application.port.FolderRepositoryPort
import com.neki.testfixture.aFolder
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify

class CreateFolderUseCaseTest :
    FunSpec({

        lateinit var folderRepository: FolderRepositoryPort
        lateinit var useCase: CreateFolderUseCase

        beforeTest {
            folderRepository = mockk()
            useCase = CreateFolderUseCase(folderRepository)
        }

        test("이름이 고유한 경우 폴더 저장 후 ID 반환") {
            // Given
            val command = CreateFolderCommand(userId = 1L, name = "새 폴더")
            val savedFolder = aFolder(id = 42L, userId = 1L, name = "새 폴더")

            every { folderRepository.existsOwnedFolderName(1L, "새 폴더") } returns false
            every { folderRepository.save(any()) } returns savedFolder

            // When
            val result = useCase.execute(command)

            // Then
            result.folderId shouldBe 42L
            verify(exactly = 1) { folderRepository.save(any()) }
        }

        test("이름이 중복된 경우 CONFLICT_FOLDER 예외 발생") {
            // Given
            val command = CreateFolderCommand(userId = 1L, name = "중복 폴더")

            every { folderRepository.existsOwnedFolderName(1L, "중복 폴더") } returns true

            // When & Then
            val ex = shouldThrow<BusinessException> {
                useCase.execute(command)
            }
            ex.resultCode shouldBe ResultCode.CONFLICT_FOLDER
            verify(exactly = 0) { folderRepository.save(any()) }
        }
    })
