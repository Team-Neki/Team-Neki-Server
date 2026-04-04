package com.neki.photo.application.usecase

import com.neki.common.api.dto.ResultCode
import com.neki.common.exception.BusinessException
import com.neki.photo.application.command.UpdatePhotoCommand
import com.neki.photo.application.port.PhotoImageRepositoryPort
import com.neki.testfixture.aPhotoImage
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk

@Suppress("DEPRECATION")
class UpdatePhotoUseCaseTest : FunSpec({

    lateinit var photoImageRepository: PhotoImageRepositoryPort
    lateinit var useCase: UpdatePhotoUseCase

    beforeTest {
        photoImageRepository = mockk()
        useCase = UpdatePhotoUseCase(photoImageRepository)
    }

    test("사진이 존재하는 경우 memo 업데이트") {
        // Given
        val photo = aPhotoImage(id = 1L, userId = 1L, memo = "기존 메모")
        val command = UpdatePhotoCommand(userId = 1L, photoId = 1L, memo = "새 메모")

        every { photoImageRepository.getOwnedPhoto(1L, 1L) } returns photo

        // When
        useCase.execute(command)

        // Then
        photo.memo shouldBe "새 메모"
    }

    test("사진이 존재하지 않는 경우 NOT_FOUND 예외 발생") {
        // Given
        val command = UpdatePhotoCommand(userId = 1L, photoId = 99L, memo = "메모")

        every { photoImageRepository.getOwnedPhoto(1L, 99L) } returns null

        // When & Then
        val ex = shouldThrow<BusinessException> {
            useCase.execute(command)
        }
        ex.resultCode shouldBe ResultCode.NOT_FOUND
    }

    test("memo가 null이면 기존 memo 값 유지") {
        // Given
        val photo = aPhotoImage(id = 1L, userId = 1L, memo = "기존 메모")
        val command = UpdatePhotoCommand(userId = 1L, photoId = 1L, memo = null)

        every { photoImageRepository.getOwnedPhoto(1L, 1L) } returns photo

        // When
        useCase.execute(command)

        // Then
        photo.memo shouldBe "기존 메모"
    }
})
