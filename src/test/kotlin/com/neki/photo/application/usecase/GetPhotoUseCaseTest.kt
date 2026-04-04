package com.neki.photo.application.usecase

import com.neki.common.api.dto.ResultCode
import com.neki.common.exception.BusinessException
import com.neki.photo.application.command.GetPhotoCommand
import com.neki.photo.application.contract.MediaStorageInfo
import com.neki.photo.application.contract.PhotoWithFavorite
import com.neki.photo.application.port.MediaClientPort
import com.neki.photo.application.port.PhotoImageRepositoryPort
import com.neki.testfixture.aPhotoImage
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import java.time.LocalDateTime

class GetPhotoUseCaseTest :
    FunSpec({

        lateinit var photoImageRepository: PhotoImageRepositoryPort
        lateinit var mediaClient: MediaClientPort
        lateinit var useCase: GetPhotoUseCase

        beforeTest {
            photoImageRepository = mockk()
            mediaClient = mockk()
            useCase = GetPhotoUseCase(photoImageRepository, mediaClient)
        }

        test("사진과 미디어 정보 정상 조회 및 매핑") {
            // Given
            val photo = aPhotoImage(id = 1L, userId = 1L, mediaId = 10L, memo = "테스트 메모").also {
                it.createdAt = LocalDateTime.of(2026, 1, 1, 12, 0)
            }
            val command = GetPhotoCommand(userId = 1L, photoId = 1L)
            val mediaInfo = MediaStorageInfo(
                mediaId = 10L,
                storageKey = "key/image.jpg",
                contentType = "image/jpeg",
                width = 800,
                height = 600,
            )

            every { photoImageRepository.getOwnedPhotoWithFavorite(1L, 1L) } returns
                PhotoWithFavorite(photo, isFavorite = true)
            every { mediaClient.getMediaStorageInfo(1L, 10L) } returns mediaInfo

            // When
            val result = useCase.execute(command)

            // Then
            result.photoId shouldBe 1L
            result.storageKey shouldBe "key/image.jpg"
            result.favorite shouldBe true
            result.contentType shouldBe "image/jpeg"
            result.memo shouldBe "테스트 메모"
            result.width shouldBe 800
            result.height shouldBe 600
        }

        test("사진이 존재하지 않는 경우 NOT_FOUND 예외 발생") {
            // Given
            val command = GetPhotoCommand(userId = 1L, photoId = 99L)

            every { photoImageRepository.getOwnedPhotoWithFavorite(1L, 99L) } returns null

            // When & Then
            val ex = shouldThrow<BusinessException> {
                useCase.execute(command)
            }
            ex.resultCode shouldBe ResultCode.NOT_FOUND
        }

        test("사진은 존재하지만 미디어 조회 실패 시 예외 전파") {
            // Given
            val photo = aPhotoImage(id = 1L, userId = 1L, mediaId = 10L).also {
                it.createdAt = LocalDateTime.of(2026, 1, 1, 12, 0)
            }
            val command = GetPhotoCommand(userId = 1L, photoId = 1L)

            every { photoImageRepository.getOwnedPhotoWithFavorite(1L, 1L) } returns
                PhotoWithFavorite(photo, isFavorite = false)
            every { mediaClient.getMediaStorageInfo(1L, 10L) } throws RuntimeException("미디어 서버 오류")

            // When & Then
            shouldThrow<RuntimeException> {
                useCase.execute(command)
            }
        }
    })
