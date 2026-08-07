package com.neki.media.application.usecase

import com.neki.media.application.DeleteMediaUseCase
import com.neki.media.dto.MediaCommand
import com.neki.media.external.MediaBinaryCache
import com.neki.media.models.MediaStatus
import com.neki.media.repository.MediaRepository
import com.neki.media.service.MediaBinaryService
import com.neki.media.service.MediaService
import com.neki.testfixture.FakeTransactionRunner
import com.neki.testfixture.aMedia
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

/**
 * fileName       : DeleteMediaUseCaseTest
 * description    : DeleteMediaUseCase 단위 테스트
 */
class DeleteMediaUseCaseTest {

    private lateinit var mediaRepository: MediaRepository
    private lateinit var cache: MediaBinaryCache
    private lateinit var useCase: DeleteMediaUseCase

    @BeforeEach
    fun setUp() {
        mediaRepository = mockk()
        cache = mockk()
        useCase = DeleteMediaUseCase(
            mediaService = MediaService(mediaRepository, mockk()),
            mediaBinaryService = MediaBinaryService(cache, mockk(), mockk()),
            transactionRunner = FakeTransactionRunner(),
        )
    }

    @Test
    @DisplayName("단건 삭제 - 한 건짜리 리스트로 호출하면 soft-delete 후 cache evict")
    fun `단건 삭제 - 한 건짜리 리스트로 호출하면 soft-delete 후 cache evict`() {
        // Given
        val ownerId = 1L
        val mediaId = 10L
        val media = aMedia(id = mediaId, ownerId = ownerId, storageKey = "pose/test.jpg")

        every { mediaRepository.getActiveMedias(ownerId, listOf(mediaId)) } returns listOf(media)
        every { mediaRepository.saveAll(listOf(media)) } returns listOf(media)
        every { cache.evict("pose/test.jpg") } just Runs

        // When
        useCase.execute(MediaCommand.DeleteMedias(ownerId = ownerId, mediaIds = listOf(mediaId)))

        // Then
        media.status shouldBe MediaStatus.DELETED
        verify(exactly = 1) { mediaRepository.saveAll(listOf(media)) }
        verify(exactly = 1) { cache.evict("pose/test.jpg") }
    }

    @Test
    @DisplayName("미디어 미존재 - 예외 없이 아무것도 하지 않는다")
    fun `미디어 미존재 - 예외 없이 아무것도 하지 않는다`() {
        // Given
        val ownerId = 1L
        val mediaId = 999L

        every { mediaRepository.getActiveMedias(ownerId, listOf(mediaId)) } returns emptyList()
        every { mediaRepository.saveAll(emptyList()) } returns emptyList()

        // When
        val result = useCase.execute(MediaCommand.DeleteMedias(ownerId = ownerId, mediaIds = listOf(mediaId)))

        // Then
        result.mediaIds shouldBe emptyList()
        verify(exactly = 0) { cache.evict(any()) }
    }

    @Test
    @DisplayName("벌크 삭제 - 모두 soft-delete 후 cache evict all")
    fun `벌크 삭제 - 모두 soft-delete 후 cache evict all`() {
        // Given
        val ownerId = 1L
        val mediaIds = listOf(1L, 2L, 3L)
        val medias = mediaIds.map { aMedia(id = it, ownerId = ownerId, storageKey = "pose/test-$it.jpg") }

        every { mediaRepository.getActiveMedias(ownerId, mediaIds) } returns medias
        every { mediaRepository.saveAll(medias) } returns medias
        medias.forEach { every { cache.evict(it.storageKey) } just Runs }

        // When
        useCase.execute(MediaCommand.DeleteMedias(ownerId = ownerId, mediaIds = mediaIds))

        // Then
        medias.forEach { it.status shouldBe MediaStatus.DELETED }
        verify(exactly = 1) { mediaRepository.saveAll(medias) }
        medias.forEach { verify(exactly = 1) { cache.evict(it.storageKey) } }
    }

    @Test
    @DisplayName("벌크 삭제 중 cache evict 실패 - N번째 evict 예외 발생 시 전파")
    fun `벌크 삭제 중 cache evict 실패 - N번째 evict 예외 발생 시 전파`() {
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
            useCase.execute(MediaCommand.DeleteMedias(ownerId = ownerId, mediaIds = mediaIds))
        }
    }

    @Test
    @DisplayName("빈 mediaIds로 벌크 삭제 - no-op")
    fun `빈 mediaIds로 벌크 삭제 - no-op`() {
        // Given
        val ownerId = 1L
        val mediaIds = emptyList<Long>()

        every { mediaRepository.getActiveMedias(ownerId, mediaIds) } returns emptyList()
        every { mediaRepository.saveAll(emptyList()) } returns emptyList()

        // When
        useCase.execute(MediaCommand.DeleteMedias(ownerId = ownerId, mediaIds = mediaIds))

        // Then
        verify(exactly = 0) { cache.evict(any()) }
    }
}
