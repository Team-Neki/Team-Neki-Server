package com.neki.photo.application.usecase

import com.neki.common.api.dto.ResultCode
import com.neki.common.exception.BusinessException
import com.neki.photo.application.command.UpdatePhotoFavoriteCommand
import com.neki.photo.application.port.FavoriteImageRepositoryPort
import com.neki.photo.application.port.PhotoImageRepositoryPort
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify

class UpdatePhotoFavoriteUseCaseTest : FunSpec({

    lateinit var photoImageRepository: PhotoImageRepositoryPort
    lateinit var favoriteImageRepository: FavoriteImageRepositoryPort
    lateinit var useCase: UpdatePhotoFavoriteUseCase

    beforeTest {
        photoImageRepository = mockk()
        favoriteImageRepository = mockk()
        useCase = UpdatePhotoFavoriteUseCase(photoImageRepository, favoriteImageRepository)
    }

    test("즐겨찾기 추가 요청 시 add 호출") {
        // Given
        val command = UpdatePhotoFavoriteCommand(userId = 1L, photoId = 1L, favorite = true)

        every { photoImageRepository.existsOwnedPhoto(1L, 1L) } returns true
        every { favoriteImageRepository.add(1L, 1L) } just Runs

        // When
        useCase.execute(command)

        // Then
        verify(exactly = 1) { favoriteImageRepository.add(1L, 1L) }
        verify(exactly = 0) { favoriteImageRepository.delete(any(), any()) }
    }

    test("즐겨찾기 해제 요청 시 delete 호출") {
        // Given
        val command = UpdatePhotoFavoriteCommand(userId = 1L, photoId = 1L, favorite = false)

        every { photoImageRepository.existsOwnedPhoto(1L, 1L) } returns true
        every { favoriteImageRepository.delete(1L, 1L) } just Runs

        // When
        useCase.execute(command)

        // Then
        verify(exactly = 1) { favoriteImageRepository.delete(1L, 1L) }
        verify(exactly = 0) { favoriteImageRepository.add(any(), any()) }
    }

    test("사진이 존재하지 않는 경우 NOT_FOUND 예외 발생") {
        // Given
        val command = UpdatePhotoFavoriteCommand(userId = 1L, photoId = 99L, favorite = true)

        every { photoImageRepository.existsOwnedPhoto(1L, 99L) } returns false

        // When & Then
        val ex = shouldThrow<BusinessException> {
            useCase.execute(command)
        }
        ex.resultCode shouldBe ResultCode.NOT_FOUND
        verify(exactly = 0) { favoriteImageRepository.add(any(), any()) }
        verify(exactly = 0) { favoriteImageRepository.delete(any(), any()) }
    }
})
