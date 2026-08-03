package com.neki.media.application.usecase

import com.neki.media.MediaRepository
import com.neki.media.application.GetMediaMetadataListUseCase
import com.neki.media.dto.MediaQuery
import com.neki.media.service.MediaService
import com.neki.testfixture.aMedia
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

/**
 * fileName       : GetMediaMetadataListUseCaseTest
 * description    : GetMediaMetadataListUseCase 단위 테스트 (복수형 벌크 조회)
 */
class GetMediaMetadataListUseCaseTest {

    private lateinit var mediaRepository: MediaRepository
    private lateinit var useCase: GetMediaMetadataListUseCase

    @BeforeEach
    fun setUp() {
        mediaRepository = mockk()
        useCase = GetMediaMetadataListUseCase(MediaService(mediaRepository, mockk()))
    }

    @Test
    @DisplayName("ownerId 포함 벌크 조회 - 결과 리스트 반환")
    fun `ownerId 포함 벌크 조회 - 결과 리스트 반환`() {
        // Given
        val ownerId = 1L
        val mediaIds = listOf(1L, 2L, 3L)
        val medias = mediaIds.map { aMedia(id = it, ownerId = ownerId, storageKey = "pose/test-$it.jpg") }

        every { mediaRepository.getActiveMedias(ownerId, mediaIds) } returns medias

        // When
        val query = MediaQuery.GetMediaMetadataList(ownerId = ownerId, mediaIds = mediaIds)
        val result = useCase.execute(query)

        // Then
        result shouldHaveSize 3
        result[0].id shouldBe 1L
        result[1].id shouldBe 2L
        result[2].id shouldBe 3L
        verify(exactly = 1) { mediaRepository.getActiveMedias(ownerId, mediaIds) }
    }

    @Test
    @DisplayName("ownerId 없이 벌크 조회 - unscoped 쿼리 사용")
    fun `ownerId 없이 벌크 조회 - unscoped 쿼리 사용`() {
        // Given
        val mediaIds = listOf(1L, 2L)
        val medias = mediaIds.map { aMedia(id = it, storageKey = "pose/test-$it.jpg") }

        every { mediaRepository.getActiveMedias(mediaIds) } returns medias

        // When
        val query = MediaQuery.GetMediaMetadataList(ownerId = null, mediaIds = mediaIds)
        val result = useCase.execute(query)

        // Then
        result shouldHaveSize 2
        verify(exactly = 0) { mediaRepository.getActiveMedias(any<Long>(), any<List<Long>>()) }
        verify(exactly = 1) { mediaRepository.getActiveMedias(mediaIds) }
    }

    @Test
    @DisplayName("빈 mediaIds 목록 조회 - 빈 결과 반환")
    fun `빈 mediaIds 목록 조회 - 빈 결과 반환`() {
        // Given
        val ownerId = 1L
        val mediaIds = emptyList<Long>()

        every { mediaRepository.getActiveMedias(ownerId, mediaIds) } returns emptyList()

        // When
        val query = MediaQuery.GetMediaMetadataList(ownerId = ownerId, mediaIds = mediaIds)
        val result = useCase.execute(query)

        // Then
        result shouldHaveSize 0
    }

    @Test
    @DisplayName("부분 결과 - 5개 요청 시 3개만 존재하면 3개만 반환")
    fun `부분 결과 - 5개 요청 시 3개만 존재하면 3개만 반환`() {
        // Given
        val ownerId = 1L
        val mediaIds = listOf(1L, 2L, 3L, 4L, 5L)
        val existingMedias = listOf(1L, 3L, 5L).map { aMedia(id = it, ownerId = ownerId) }

        every { mediaRepository.getActiveMedias(ownerId, mediaIds) } returns existingMedias

        // When
        val query = MediaQuery.GetMediaMetadataList(ownerId = ownerId, mediaIds = mediaIds)
        val result = useCase.execute(query)

        // Then
        result shouldHaveSize 3
        result.map { it.id } shouldBe listOf(1L, 3L, 5L)
    }
}
