package com.neki.media.application.usecase

import com.neki.common.api.dto.ResultCode
import com.neki.common.exception.BusinessException
import com.neki.media.application.command.DeleteMediaCommand
import com.neki.media.application.command.DeleteMediasCommand
import com.neki.media.application.port.MediaBinaryCachePort
import com.neki.media.application.port.MediaRepositoryPort
import com.neki.media.domain.entity.MediaStatus
import com.neki.testfixture.FakeTransactionRunner
import com.neki.testfixture.aMedia
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify

/**
 * fileName       : DeleteMediaUseCaseTest
 * description    : DeleteMediaUseCase 단위 테스트
 */
class DeleteMediaUseCaseTest :
    FunSpec({

        lateinit var mediaRepository: MediaRepositoryPort
        lateinit var cache: MediaBinaryCachePort
        lateinit var useCase: DeleteMediaUseCase

        beforeTest {
            mediaRepository = mockk()
            cache = mockk()
            useCase = DeleteMediaUseCase(
                mediaRepository = mediaRepository,
                cache = cache,
                transactionRunner = FakeTransactionRunner(),
            )
        }

        test("단건 삭제 - 미디어 존재 시 soft-delete 후 cache evict") {
            // Given
            val ownerId = 1L
            val mediaId = 10L
            val media = aMedia(id = mediaId, ownerId = ownerId, storageKey = "pose/test.jpg")

            every { mediaRepository.getActiveMedia(ownerId, mediaId) } returns media
            every { mediaRepository.save(media) } returns media
            every { cache.evict("pose/test.jpg") } just Runs

            // When
            useCase.execute(DeleteMediaCommand(ownerId = ownerId, mediaId = mediaId))

            // Then
            media.status shouldBe MediaStatus.DELETED
            verify(exactly = 1) { mediaRepository.save(media) }
            verify(exactly = 1) { cache.evict("pose/test.jpg") }
        }

        test("단건 삭제 - 미디어 미존재 시 BusinessException(NOT_FOUND) 발생") {
            // Given
            val ownerId = 1L
            val mediaId = 999L

            every { mediaRepository.getActiveMedia(ownerId, mediaId) } returns null

            // When & Then
            val exception = shouldThrow<BusinessException> {
                useCase.execute(DeleteMediaCommand(ownerId = ownerId, mediaId = mediaId))
            }
            exception.resultCode shouldBe ResultCode.NOT_FOUND
            verify(exactly = 0) { cache.evict(any()) }
        }

        test("벌크 삭제 - 모두 soft-delete 후 cache evict all") {
            // Given
            val ownerId = 1L
            val mediaIds = listOf(1L, 2L, 3L)
            val medias = mediaIds.map { aMedia(id = it, ownerId = ownerId, storageKey = "pose/test-$it.jpg") }

            every { mediaRepository.getActiveMedias(ownerId, mediaIds) } returns medias
            every { mediaRepository.saveAll(medias) } returns medias
            medias.forEach { every { cache.evict(it.storageKey) } just Runs }

            // When
            useCase.execute(DeleteMediasCommand(ownerId = ownerId, mediaIds = mediaIds))

            // Then
            medias.forEach { it.status shouldBe MediaStatus.DELETED }
            verify(exactly = 1) { mediaRepository.saveAll(medias) }
            medias.forEach { verify(exactly = 1) { cache.evict(it.storageKey) } }
        }

        test("벌크 삭제 중 cache evict 실패 - N번째 evict 예외 발생 시 전파") {
            // Given
            val ownerId = 1L
            val mediaIds = listOf(1L, 2L, 3L)
            val medias = mediaIds.map { aMedia(id = it, ownerId = ownerId, storageKey = "pose/test-$it.jpg") }

            every { mediaRepository.getActiveMedias(ownerId, mediaIds) } returns medias
            every { mediaRepository.saveAll(medias) } returns medias
            every { cache.evict("pose/test-1.jpg") } just Runs
            every { cache.evict("pose/test-2.jpg") } throws RuntimeException("Cache evict 실패")

            // When & Then
            shouldThrow<RuntimeException> {
                useCase.execute(DeleteMediasCommand(ownerId = ownerId, mediaIds = mediaIds))
            }
        }

        test("빈 mediaIds로 벌크 삭제 - no-op") {
            // Given
            val ownerId = 1L
            val mediaIds = emptyList<Long>()

            every { mediaRepository.getActiveMedias(ownerId, mediaIds) } returns emptyList()
            every { mediaRepository.saveAll(emptyList()) } returns emptyList()

            // When
            useCase.execute(DeleteMediasCommand(ownerId = ownerId, mediaIds = mediaIds))

            // Then
            verify(exactly = 0) { cache.evict(any()) }
        }
    })
