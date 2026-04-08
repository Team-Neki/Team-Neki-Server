package com.neki.photo.application.usecase

import com.neki.common.api.dto.ResultCode
import com.neki.common.exception.BusinessException
import com.neki.photo.application.command.RemovePhotosFromFolderCommand
import com.neki.photo.application.port.FolderRepositoryPort
import com.neki.photo.application.port.PhotoImageFolderRepositoryPort
import com.neki.photo.application.port.PhotoImageRepositoryPort
import com.neki.testfixture.aFolder
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

class RemovePhotosFromFolderUseCaseTest {

    lateinit var folderRepository: FolderRepositoryPort
    lateinit var photoImageRepository: PhotoImageRepositoryPort
    lateinit var photoImageFolderRepository: PhotoImageFolderRepositoryPort
    lateinit var useCase: RemovePhotosFromFolderUseCase

    @BeforeEach
    fun setUp() {
        folderRepository = mockk()
        photoImageRepository = mockk()
        photoImageFolderRepository = mockk()
        useCase = RemovePhotosFromFolderUseCase(folderRepository, photoImageRepository, photoImageFolderRepository)
    }

    @Test
    @DisplayName("폴더 소유 확인 후 사진 folderId 제거 및 중간 테이블 삭제")
    fun `폴더 소유 확인 후 사진 folderId 제거 및 중간 테이블 삭제`() {
        // Given
        val folder = aFolder(id = 1L, userId = 1L)
        val photoIds = listOf(10L, 20L)
        val command = RemovePhotosFromFolderCommand(userId = 1L, folderId = 1L, photoIds = photoIds)

        every { folderRepository.getOwnedFolder(1L, 1L) } returns folder
        every { photoImageRepository.removePhotosFromFolder(1L, 1L, photoIds) } returns 2
        every { photoImageFolderRepository.deleteByPhotoImageIdsAndFolderId(photoIds, 1L) } returns Unit

        // When
        useCase.execute(command)

        // Then
        verify(exactly = 1) { photoImageRepository.removePhotosFromFolder(1L, 1L, photoIds) }
        verify(exactly = 1) { photoImageFolderRepository.deleteByPhotoImageIdsAndFolderId(photoIds, 1L) }
    }

    @Test
    @DisplayName("폴더 소유권이 없는 경우 NOT_FOUND 예외 발생")
    fun `폴더 소유권이 없는 경우 NOT_FOUND 예외 발생`() {
        // Given
        val command = RemovePhotosFromFolderCommand(userId = 1L, folderId = 99L, photoIds = listOf(10L))

        every { folderRepository.getOwnedFolder(1L, 99L) } returns null

        // When & Then
        val ex = shouldThrow<BusinessException> {
            useCase.execute(command)
        }
        ex.resultCode shouldBe ResultCode.NOT_FOUND
        verify(exactly = 0) { photoImageRepository.removePhotosFromFolder(any(), any(), any()) }
        verify(exactly = 0) { photoImageFolderRepository.deleteByPhotoImageIdsAndFolderId(any(), any()) }
    }

    @Test
    @DisplayName("빈 photoIds 리스트인 경우 멱등적으로 처리")
    fun `빈 photoIds 리스트인 경우 멱등적으로 처리`() {
        // Given
        val folder = aFolder(id = 1L, userId = 1L)
        val emptyPhotoIds = emptyList<Long>()
        val command = RemovePhotosFromFolderCommand(userId = 1L, folderId = 1L, photoIds = emptyPhotoIds)

        every { folderRepository.getOwnedFolder(1L, 1L) } returns folder
        every { photoImageRepository.removePhotosFromFolder(1L, 1L, emptyPhotoIds) } returns 0
        every { photoImageFolderRepository.deleteByPhotoImageIdsAndFolderId(emptyPhotoIds, 1L) } returns Unit

        // When
        useCase.execute(command)

        // Then
        verify(exactly = 1) { photoImageRepository.removePhotosFromFolder(1L, 1L, emptyPhotoIds) }
        verify(exactly = 1) { photoImageFolderRepository.deleteByPhotoImageIdsAndFolderId(emptyPhotoIds, 1L) }
    }
}
