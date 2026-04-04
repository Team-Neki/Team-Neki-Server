package com.neki.photo.application.usecase

import com.neki.common.api.dto.ResultCode
import com.neki.common.exception.BusinessException
import com.neki.photo.application.command.DeleteFoldersCommand
import com.neki.photo.application.port.FavoriteImageRepositoryPort
import com.neki.photo.application.port.FolderRepositoryPort
import com.neki.photo.application.port.MediaClientPort
import com.neki.photo.application.port.PhotoImageFolderRepositoryPort
import com.neki.photo.application.port.PhotoImageRepositoryPort
import com.neki.testfixture.FakeTransactionRunner
import com.neki.testfixture.aPhotoImage
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify

class DeleteFoldersUseCaseTest : FunSpec({

    lateinit var folderRepository: FolderRepositoryPort
    lateinit var photoImageRepository: PhotoImageRepositoryPort
    lateinit var photoImageFolderRepository: PhotoImageFolderRepositoryPort
    lateinit var favoriteImageRepository: FavoriteImageRepositoryPort
    lateinit var mediaClient: MediaClientPort
    lateinit var useCase: DeleteFoldersUseCase

    beforeTest {
        folderRepository = mockk()
        photoImageRepository = mockk()
        photoImageFolderRepository = mockk()
        favoriteImageRepository = mockk()
        mediaClient = mockk()
        useCase = DeleteFoldersUseCase(
            folderRepository,
            photoImageRepository,
            photoImageFolderRepository,
            favoriteImageRepository,
            mediaClient,
            FakeTransactionRunner(),
        )
    }

    test("deletePhotos=true이면 즐겨찾기→사진→폴더 삭제 후 media cleanup 호출") {
        // Given
        val folderIds = listOf(1L)
        val photoIds = listOf(10L, 20L)
        val command = DeleteFoldersCommand(userId = 1L, folderIds = folderIds, deletePhotos = true)
        val deletedPhotos = listOf(
            aPhotoImage(id = 10L, userId = 1L, mediaId = 100L),
            aPhotoImage(id = 20L, userId = 1L, mediaId = 200L),
        )

        every { photoImageRepository.getPhotoIdsByFolderIds(1L, folderIds) } returns photoIds
        every { favoriteImageRepository.deleteAll(1L, photoIds) } just Runs
        every { photoImageFolderRepository.deleteByFolderIds(folderIds) } just Runs
        every { folderRepository.deleteOwnedFolders(1L, folderIds) } returns 1
        every { photoImageRepository.deleteOwnedPhotos(1L, photoIds) } returns deletedPhotos
        every { mediaClient.deleteMedias(1L, listOf(100L, 200L)) } just Runs

        // When
        useCase.execute(command)

        // Then
        verify(exactly = 1) { favoriteImageRepository.deleteAll(1L, photoIds) }
        verify(exactly = 1) { photoImageRepository.deleteOwnedPhotos(1L, photoIds) }
        verify(exactly = 1) { mediaClient.deleteMedias(1L, listOf(100L, 200L)) }
    }

    test("deletePhotos=false이면 사진 folderId를 null로 업데이트하고 폴더 삭제, media 미호출") {
        // Given
        val folderIds = listOf(1L)
        val command = DeleteFoldersCommand(userId = 1L, folderIds = folderIds, deletePhotos = false)

        every { photoImageRepository.updatePhotosFolderIdToNull(1L, folderIds) } returns 5
        every { photoImageFolderRepository.deleteByFolderIds(folderIds) } just Runs
        every { folderRepository.deleteOwnedFolders(1L, folderIds) } returns 1

        // When
        useCase.execute(command)

        // Then
        verify(exactly = 1) { photoImageRepository.updatePhotosFolderIdToNull(1L, folderIds) }
        verify(exactly = 1) { folderRepository.deleteOwnedFolders(1L, folderIds) }
        verify(exactly = 0) { mediaClient.deleteMedias(any(), any()) }
        verify(exactly = 0) { favoriteImageRepository.deleteAll(any(), any()) }
    }

    test("폴더 카운트 불일치 시 NOT_FOUND 예외 발생") {
        // Given
        val folderIds = listOf(1L, 2L)
        val command = DeleteFoldersCommand(userId = 1L, folderIds = folderIds, deletePhotos = false)

        every { photoImageRepository.updatePhotosFolderIdToNull(1L, folderIds) } returns 0
        every { photoImageFolderRepository.deleteByFolderIds(folderIds) } just Runs
        // 2개를 요청했는데 1개만 삭제됨
        every { folderRepository.deleteOwnedFolders(1L, folderIds) } returns 1

        // When & Then
        val ex = shouldThrow<BusinessException> {
            useCase.execute(command)
        }
        ex.resultCode shouldBe ResultCode.NOT_FOUND
    }

    test("deletePhotos=true이고 빈 폴더인 경우 photoIds가 비어있어 media cleanup 미호출") {
        // Given
        val folderIds = listOf(1L)
        val command = DeleteFoldersCommand(userId = 1L, folderIds = folderIds, deletePhotos = true)

        every { photoImageRepository.getPhotoIdsByFolderIds(1L, folderIds) } returns emptyList()
        every { photoImageFolderRepository.deleteByFolderIds(folderIds) } just Runs
        every { folderRepository.deleteOwnedFolders(1L, folderIds) } returns 1

        // When
        useCase.execute(command)

        // Then
        verify(exactly = 0) { favoriteImageRepository.deleteAll(any(), any()) }
        verify(exactly = 0) { photoImageRepository.deleteOwnedPhotos(any(), any()) }
        verify(exactly = 0) { mediaClient.deleteMedias(any(), any()) }
    }

    test("media cleanup 실패 시 DB 삭제 성공 후 예외 전파") {
        // Given
        val folderIds = listOf(1L)
        val photoIds = listOf(10L)
        val command = DeleteFoldersCommand(userId = 1L, folderIds = folderIds, deletePhotos = true)
        val deletedPhotos = listOf(aPhotoImage(id = 10L, userId = 1L, mediaId = 100L))

        every { photoImageRepository.getPhotoIdsByFolderIds(1L, folderIds) } returns photoIds
        every { favoriteImageRepository.deleteAll(1L, photoIds) } just Runs
        every { photoImageFolderRepository.deleteByFolderIds(folderIds) } just Runs
        every { folderRepository.deleteOwnedFolders(1L, folderIds) } returns 1
        every { photoImageRepository.deleteOwnedPhotos(1L, photoIds) } returns deletedPhotos
        every { mediaClient.deleteMedias(1L, listOf(100L)) } throws RuntimeException("미디어 삭제 실패")

        // When & Then
        shouldThrow<RuntimeException> {
            useCase.execute(command)
        }
        verify(exactly = 1) { folderRepository.deleteOwnedFolders(1L, folderIds) }
        verify(exactly = 1) { photoImageRepository.deleteOwnedPhotos(1L, photoIds) }
    }
})
