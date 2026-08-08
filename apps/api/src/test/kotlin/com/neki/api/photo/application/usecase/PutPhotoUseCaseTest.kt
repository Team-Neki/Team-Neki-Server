package com.neki.api.photo.application.usecase

import com.neki.api.photo.application.PutPhotoUseCase
import com.neki.api.testfixture.aPhotoImage
import com.neki.core.code.ResultCode
import com.neki.core.exception.BusinessException
import com.neki.domain.photo.dto.PhotoImageCommand
import com.neki.domain.photo.repository.PhotoImageRepository
import com.neki.domain.photo.service.PhotoService
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

class PutPhotoUseCaseTest {

    lateinit var photoImageRepository: PhotoImageRepository
    lateinit var useCase: PutPhotoUseCase

    @BeforeEach
    fun setUp() {
        photoImageRepository = mockk()
        useCase = PutPhotoUseCase(PhotoService(photoImageRepository))
    }

    @Test
    @DisplayName("사진이 존재하는 경우 memo와 capturedAt 업데이트")
    fun `사진이 존재하는 경우 memo와 capturedAt 업데이트`() {
        // Given
        val photo = aPhotoImage(id = 1L, userId = 1L, memo = "기존 메모")
        val capturedAt = LocalDateTime.of(2026, 1, 1, 12, 0)
        val command = PhotoImageCommand.PutPhoto(userId = 1L, photoId = 1L, memo = "새 메모", capturedAt = capturedAt)

        every { photoImageRepository.getOwnedPhoto(1L, 1L) } returns photo

        // When
        useCase.execute(command)

        // Then
        photo.memo shouldBe "새 메모"
        photo.capturedAt shouldBe capturedAt
    }

    @Test
    @DisplayName("사진이 존재하지 않는 경우 NOT_FOUND 예외 발생")
    fun `사진이 존재하지 않는 경우 NOT_FOUND 예외 발생`() {
        // Given
        val command = PhotoImageCommand.PutPhoto(userId = 1L, photoId = 99L, memo = "메모", capturedAt = null)

        every { photoImageRepository.getOwnedPhoto(1L, 99L) } returns null

        // When & Then
        val ex = shouldThrow<BusinessException> {
            useCase.execute(command)
        }
        ex.resultCode shouldBe ResultCode.NOT_FOUND
    }

    @Test
    @DisplayName("memo가 null인 경우 null로 업데이트")
    fun `memo가 null인 경우 null로 업데이트`() {
        // Given
        val photo = aPhotoImage(id = 1L, userId = 1L, memo = "기존 메모")
        val command = PhotoImageCommand.PutPhoto(userId = 1L, photoId = 1L, memo = null, capturedAt = null)

        every { photoImageRepository.getOwnedPhoto(1L, 1L) } returns photo

        // When
        useCase.execute(command)

        // Then
        photo.memo shouldBe null
        photo.capturedAt shouldBe null
    }
}
