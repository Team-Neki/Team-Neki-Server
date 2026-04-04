package com.neki.media.application.usecase

import com.neki.common.api.dto.ResultCode
import com.neki.common.exception.BusinessException
import com.neki.media.application.command.GetMediaStorageInfoCommand
import com.neki.media.application.port.MediaRepositoryPort
import com.neki.testfixture.aMedia
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify

/**
 * fileName       : GetMediaStorageInfoUseCaseTest
 * description    : GetMediaStorageInfoUseCase 단위 테스트
 */
class GetMediaStorageInfoUseCaseTest :
    FunSpec({

        lateinit var mediaRepository: MediaRepositoryPort
        lateinit var useCase: GetMediaStorageInfoUseCase

        beforeTest {
            mediaRepository = mockk()
            useCase = GetMediaStorageInfoUseCase(mediaRepository)
        }

        test("ownerId 포함 조회 - scoped query 로 결과 반환") {
            // Given
            val ownerId = 1L
            val mediaId = 10L
            val media = aMedia(id = mediaId, ownerId = ownerId, storageKey = "pose/test.jpg")

            every { mediaRepository.getActiveMedia(ownerId, mediaId) } returns media

            // When
            val command = GetMediaStorageInfoCommand(ownerId = ownerId, mediaId = mediaId)
            val result = useCase.execute(command)

            // Then
            result.mediaId shouldBe mediaId
            result.storageKey shouldBe "pose/test.jpg"
            verify(exactly = 1) { mediaRepository.getActiveMedia(ownerId, mediaId) }
            verify(exactly = 0) { mediaRepository.getActiveMedia(mediaId) }
        }

        test("ownerId 없이 조회 - unscoped query 사용") {
            // Given
            val mediaId = 10L
            val media = aMedia(id = mediaId, storageKey = "pose/test.jpg")

            every { mediaRepository.getActiveMedia(mediaId) } returns media

            // When
            val command = GetMediaStorageInfoCommand(ownerId = null, mediaId = mediaId)
            val result = useCase.execute(command)

            // Then
            result.mediaId shouldBe mediaId
            result.storageKey shouldBe "pose/test.jpg"
            verify(exactly = 0) { mediaRepository.getActiveMedia(any<Long>(), any<Long>()) }
            verify(exactly = 1) { mediaRepository.getActiveMedia(mediaId) }
        }

        test("미존재 미디어 조회 시 BusinessException(NOT_FOUND) 발생") {
            // Given: ownerId 없이 조회하는 경우를 테스트 (unscoped)
            val mediaId = 999L

            every { mediaRepository.getActiveMedia(mediaId) } returns null

            // When & Then
            val command = GetMediaStorageInfoCommand(ownerId = null, mediaId = mediaId)
            val exception = shouldThrow<BusinessException> {
                useCase.execute(command)
            }
            exception.resultCode shouldBe ResultCode.NOT_FOUND
        }
    })
