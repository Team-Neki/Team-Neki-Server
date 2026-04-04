package com.neki.photo.application.usecase

import com.neki.photo.application.command.DeletePhotosCommand
import com.neki.photo.application.port.FavoriteImageRepositoryPort
import com.neki.photo.application.port.MediaClientPort
import com.neki.photo.application.port.PhotoImageRepositoryPort
import com.neki.testfixture.FakeTransactionRunner
import com.neki.testfixture.aPhotoImage
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify

class DeletePhotosUseCaseTest : FunSpec({

    lateinit var photoImageRepository: PhotoImageRepositoryPort
    lateinit var favoriteImageRepository: FavoriteImageRepositoryPort
    lateinit var mediaClient: MediaClientPort
    lateinit var useCase: DeletePhotosUseCase

    beforeTest {
        photoImageRepository = mockk()
        favoriteImageRepository = mockk()
        mediaClient = mockk()
        useCase = DeletePhotosUseCase(
            photoImageRepository,
            favoriteImageRepository,
            mediaClient,
            FakeTransactionRunner(),
        )
    }

    test("정상 삭제 - 즐겨찾기 삭제 후 사진 삭제 후 media cleanup 호출") {
        // Given
        val photoIds = listOf(1L, 2L)
        val command = DeletePhotosCommand(userId = 1L, photoIds = photoIds)
        val deletedPhotos = listOf(
            aPhotoImage(id = 1L, userId = 1L, mediaId = 10L),
            aPhotoImage(id = 2L, userId = 1L, mediaId = 20L),
        )

        every { favoriteImageRepository.deleteAll(1L, photoIds) } just Runs
        every { photoImageRepository.deleteOwnedPhotos(1L, photoIds) } returns deletedPhotos
        every { mediaClient.deleteMedias(1L, listOf(10L, 20L)) } just Runs

        // When
        useCase.execute(command)

        // Then
        verify(exactly = 1) { favoriteImageRepository.deleteAll(1L, photoIds) }
        verify(exactly = 1) { photoImageRepository.deleteOwnedPhotos(1L, photoIds) }
        verify(exactly = 1) { mediaClient.deleteMedias(1L, listOf(10L, 20L)) }
    }

    test("빈 photoIds 리스트인 경우 media cleanup은 빈 리스트로 호출됨") {
        // Given
        val command = DeletePhotosCommand(userId = 1L, photoIds = emptyList())

        every { favoriteImageRepository.deleteAll(1L, emptyList()) } just Runs
        every { photoImageRepository.deleteOwnedPhotos(1L, emptyList()) } returns emptyList()
        every { mediaClient.deleteMedias(1L, emptyList()) } just Runs

        // When
        useCase.execute(command)

        // Then
        verify(exactly = 1) { mediaClient.deleteMedias(1L, emptyList()) }
    }

    test("media cleanup 실패 시 DB 삭제는 성공 후 예외 전파") {
        // Given
        val photoIds = listOf(1L)
        val command = DeletePhotosCommand(userId = 1L, photoIds = photoIds)
        val deletedPhotos = listOf(aPhotoImage(id = 1L, userId = 1L, mediaId = 10L))

        every { favoriteImageRepository.deleteAll(1L, photoIds) } just Runs
        every { photoImageRepository.deleteOwnedPhotos(1L, photoIds) } returns deletedPhotos
        every { mediaClient.deleteMedias(1L, listOf(10L)) } throws RuntimeException("미디어 삭제 실패")

        // When & Then
        shouldThrow<RuntimeException> {
            useCase.execute(command)
        }
        verify(exactly = 1) { photoImageRepository.deleteOwnedPhotos(1L, photoIds) }
    }
})
