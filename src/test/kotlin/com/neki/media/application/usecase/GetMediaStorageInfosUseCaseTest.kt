package com.neki.media.application.usecase

import com.neki.media.application.command.GetMediaStorageInfosCommand
import com.neki.media.application.port.MediaRepositoryPort
import com.neki.testfixture.aMedia
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify

/**
 * fileName       : GetMediaStorageInfosUseCaseTest
 * description    : GetMediaStorageInfosUseCase 단위 테스트 (복수형 벌크 조회)
 */
class GetMediaStorageInfosUseCaseTest : FunSpec({

    lateinit var mediaRepository: MediaRepositoryPort
    lateinit var useCase: GetMediaStorageInfosUseCase

    beforeTest {
        mediaRepository = mockk()
        useCase = GetMediaStorageInfosUseCase(mediaRepository)
    }

    test("ownerId 포함 벌크 조회 - 결과 리스트 반환") {
        // Given
        val ownerId = 1L
        val mediaIds = listOf(1L, 2L, 3L)
        val medias = mediaIds.map { aMedia(id = it, ownerId = ownerId, storageKey = "pose/test-$it.jpg") }

        every { mediaRepository.getActiveMedias(ownerId, mediaIds) } returns medias

        // When
        val command = GetMediaStorageInfosCommand(ownerId = ownerId, mediaIds = mediaIds)
        val result = useCase.execute(command)

        // Then
        result.storageInfos shouldHaveSize 3
        result.storageInfos[0].mediaId shouldBe 1L
        result.storageInfos[1].mediaId shouldBe 2L
        result.storageInfos[2].mediaId shouldBe 3L
        verify(exactly = 1) { mediaRepository.getActiveMedias(ownerId, mediaIds) }
    }

    test("ownerId 없이 벌크 조회 - unscoped 쿼리 사용") {
        // Given
        val mediaIds = listOf(1L, 2L)
        val medias = mediaIds.map { aMedia(id = it, storageKey = "pose/test-$it.jpg") }

        every { mediaRepository.getActiveMedias(mediaIds) } returns medias

        // When
        val command = GetMediaStorageInfosCommand(ownerId = null, mediaIds = mediaIds)
        val result = useCase.execute(command)

        // Then
        result.storageInfos shouldHaveSize 2
        verify(exactly = 0) { mediaRepository.getActiveMedias(any<Long>(), any<List<Long>>()) }
        verify(exactly = 1) { mediaRepository.getActiveMedias(mediaIds) }
    }

    test("빈 mediaIds 목록 조회 - 빈 결과 반환") {
        // Given
        val ownerId = 1L
        val mediaIds = emptyList<Long>()

        every { mediaRepository.getActiveMedias(ownerId, mediaIds) } returns emptyList()

        // When
        val command = GetMediaStorageInfosCommand(ownerId = ownerId, mediaIds = mediaIds)
        val result = useCase.execute(command)

        // Then
        result.storageInfos shouldHaveSize 0
    }

    test("부분 결과 - 5개 요청 시 3개만 존재하면 3개만 반환") {
        // Given
        val ownerId = 1L
        val mediaIds = listOf(1L, 2L, 3L, 4L, 5L)
        val existingMedias = listOf(1L, 3L, 5L).map { aMedia(id = it, ownerId = ownerId) }

        every { mediaRepository.getActiveMedias(ownerId, mediaIds) } returns existingMedias

        // When
        val command = GetMediaStorageInfosCommand(ownerId = ownerId, mediaIds = mediaIds)
        val result = useCase.execute(command)

        // Then
        result.storageInfos shouldHaveSize 3
        result.storageInfos.map { it.mediaId } shouldBe listOf(1L, 3L, 5L)
    }
})
