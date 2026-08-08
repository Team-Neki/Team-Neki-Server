package com.neki.api.photo.application.usecase

import com.neki.api.photo.application.UpdatePhotoFavoriteUseCase
import com.neki.core.code.ResultCode
import com.neki.core.exception.BusinessException
import com.neki.domain.photo.dto.PhotoImageCommand
import com.neki.domain.photo.models.FavoritePhoto
import com.neki.domain.photo.repository.FavoriteImageRepository
import com.neki.domain.photo.repository.PhotoImageRepository
import com.neki.domain.photo.service.FavoriteService
import com.neki.domain.photo.service.PhotoService
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

class UpdatePhotoFavoriteUseCaseTest {

    lateinit var photoImageRepository: PhotoImageRepository
    lateinit var favoriteImageRepository: FavoriteImageRepository
    lateinit var useCase: UpdatePhotoFavoriteUseCase

    @BeforeEach
    fun setUp() {
        photoImageRepository = mockk()
        favoriteImageRepository = mockk()
        useCase =
            UpdatePhotoFavoriteUseCase(PhotoService(photoImageRepository), FavoriteService(favoriteImageRepository))
    }

    @Test
    @DisplayName("즐겨찾기 추가 요청 시 add 호출")
    fun `즐겨찾기 추가 요청 시 add 호출`() {
        // Given
        val command = PhotoImageCommand.UpdatePhotoFavorite(userId = 1L, photoId = 1L, favorite = true)

        val favoritePhotoSlot = slot<FavoritePhoto>()
        every { photoImageRepository.existsOwnedPhoto(1L, 1L) } returns true
        every { favoriteImageRepository.add(capture(favoritePhotoSlot)) } just Runs

        // When
        useCase.execute(command)

        // Then
        verify(exactly = 1) { favoriteImageRepository.add(any()) }
        verify(exactly = 0) { favoriteImageRepository.delete(any()) }
        favoritePhotoSlot.captured.id.userId shouldBe 1L
        favoritePhotoSlot.captured.id.photoId shouldBe 1L
    }

    @Test
    @DisplayName("즐겨찾기 해제 요청 시 delete 호출")
    fun `즐겨찾기 해제 요청 시 delete 호출`() {
        // Given
        val command = PhotoImageCommand.UpdatePhotoFavorite(userId = 1L, photoId = 1L, favorite = false)

        val favoritePhotoSlot = slot<FavoritePhoto>()
        every { photoImageRepository.existsOwnedPhoto(1L, 1L) } returns true
        every { favoriteImageRepository.delete(capture(favoritePhotoSlot)) } just Runs

        // When
        useCase.execute(command)

        // Then
        verify(exactly = 1) { favoriteImageRepository.delete(any()) }
        verify(exactly = 0) { favoriteImageRepository.add(any()) }
        favoritePhotoSlot.captured.id.userId shouldBe 1L
        favoritePhotoSlot.captured.id.photoId shouldBe 1L
    }

    @Test
    @DisplayName("사진이 존재하지 않는 경우 NOT_FOUND 예외 발생")
    fun `사진이 존재하지 않는 경우 NOT_FOUND 예외 발생`() {
        // Given
        val command = PhotoImageCommand.UpdatePhotoFavorite(userId = 1L, photoId = 99L, favorite = true)

        every { photoImageRepository.existsOwnedPhoto(1L, 99L) } returns false

        // When & Then
        val ex = shouldThrow<BusinessException> {
            useCase.execute(command)
        }
        ex.resultCode shouldBe ResultCode.NOT_FOUND
        verify(exactly = 0) { favoriteImageRepository.add(any()) }
        verify(exactly = 0) { favoriteImageRepository.delete(any()) }
    }
}
