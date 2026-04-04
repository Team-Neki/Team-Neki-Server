package com.neki.photo.application.usecase

import com.neki.common.api.dto.ResultCode
import com.neki.common.exception.BusinessException
import com.neki.photo.application.command.PutPhotoCommand
import com.neki.photo.application.port.PhotoImageRepositoryPort
import com.neki.testfixture.aPhotoImage
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import java.time.LocalDateTime

class PutPhotoUseCaseTest : FunSpec({

    lateinit var photoImageRepository: PhotoImageRepositoryPort
    lateinit var useCase: PutPhotoUseCase

    beforeTest {
        photoImageRepository = mockk()
        useCase = PutPhotoUseCase(photoImageRepository)
    }

    test("사진이 존재하는 경우 memo와 capturedAt 업데이트") {
        // Given
        val photo = aPhotoImage(id = 1L, userId = 1L, memo = "기존 메모")
        val capturedAt = LocalDateTime.of(2026, 1, 1, 12, 0)
        val command = PutPhotoCommand(userId = 1L, photoId = 1L, memo = "새 메모", capturedAt = capturedAt)

        every { photoImageRepository.getOwnedPhoto(1L, 1L) } returns photo

        // When
        useCase.execute(command)

        // Then
        photo.memo shouldBe "새 메모"
        photo.capturedAt shouldBe capturedAt
    }

    test("사진이 존재하지 않는 경우 NOT_FOUND 예외 발생") {
        // Given
        val command = PutPhotoCommand(userId = 1L, photoId = 99L, memo = "메모", capturedAt = null)

        every { photoImageRepository.getOwnedPhoto(1L, 99L) } returns null

        // When & Then
        val ex = shouldThrow<BusinessException> {
            useCase.execute(command)
        }
        ex.resultCode shouldBe ResultCode.NOT_FOUND
    }

    test("memo가 null인 경우 null로 업데이트") {
        // Given
        val photo = aPhotoImage(id = 1L, userId = 1L, memo = "기존 메모")
        val command = PutPhotoCommand(userId = 1L, photoId = 1L, memo = null, capturedAt = null)

        every { photoImageRepository.getOwnedPhoto(1L, 1L) } returns photo

        // When
        useCase.execute(command)

        // Then
        photo.memo shouldBe null
        photo.capturedAt shouldBe null
    }
})
