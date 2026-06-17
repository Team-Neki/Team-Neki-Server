package com.neki.media.application.usecase

import com.neki.media.MediaType
import com.neki.media.application.command.GetMediasCommand
import com.neki.media.application.port.MediaBinaryCachePort
import com.neki.media.application.port.MediaRepositoryPort
import com.neki.media.application.port.MediaStoragePort
import com.neki.testfixture.aMedia
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.time.Duration

/**
 * fileName       : GetMediasUseCaseTest
 * description    : GetMediasUseCase 단위 테스트
 */
class GetMediasUseCaseTest {

    private lateinit var mediaRepository: MediaRepositoryPort
    private lateinit var mediaStorage: MediaStoragePort
    private lateinit var cache: MediaBinaryCachePort
    private lateinit var useCase: GetMediasUseCase

    private val imageData = byteArrayOf(1, 2, 3)

    @BeforeEach
    fun setUp() {
        mediaRepository = mockk()
        mediaStorage = mockk()
        cache = mockk()
        useCase = GetMediasUseCase(
            mediaRepository = mediaRepository,
            mediaStorage = mediaStorage,
            cache = cache,
        )
    }

    @Test
    @DisplayName("Cacheable 타입 + cache hit - 캐시에서 반환")
    fun `Cacheable 타입 + cache hit - 캐시에서 반환`() {
        // Given: POSE는 cacheable
        val ownerId = 1L
        val mediaId = 1L
        val storageKey = "pose/test.jpg"
        val media = aMedia(id = mediaId, ownerId = ownerId, mediaType = MediaType.POSE, storageKey = storageKey)

        every { mediaRepository.getActiveMedias(ownerId, listOf(mediaId)) } returns listOf(media)
        every { cache.get(storageKey) } returns imageData

        // When
        val command = GetMediasCommand(ownerId = ownerId, mediaIds = listOf(mediaId))
        val result = useCase.execute(command)

        // Then
        result.medias shouldHaveSize 1
        result.medias[0].binaryData shouldBe imageData
        verify(exactly = 0) { mediaStorage.fetchBinaryByKey(any()) }
    }

    @Test
    @DisplayName("Cacheable 타입 + cache miss - S3 조회 후 캐싱")
    fun `Cacheable 타입 + cache miss - S3 조회 후 캐싱`() {
        // Given: POSE는 cacheable, 캐시 miss
        val ownerId = 1L
        val mediaId = 1L
        val storageKey = "pose/test.jpg"
        val media = aMedia(id = mediaId, ownerId = ownerId, mediaType = MediaType.POSE, storageKey = storageKey)

        every { mediaRepository.getActiveMedias(ownerId, listOf(mediaId)) } returns listOf(media)
        every { cache.get(storageKey) } returns null
        every { mediaStorage.fetchBinaryByKey(storageKey) } returns imageData
        every { cache.put(storageKey, imageData, any<Duration>()) } returns Unit

        // When
        val command = GetMediasCommand(ownerId = ownerId, mediaIds = listOf(mediaId))
        val result = useCase.execute(command)

        // Then
        result.medias shouldHaveSize 1
        result.medias[0].binaryData shouldBe imageData
        verify(exactly = 1) { mediaStorage.fetchBinaryByKey(storageKey) }
        verify(exactly = 1) { cache.put(storageKey, imageData, any<Duration>()) }
    }

    @Test
    @DisplayName("Non-cacheable 타입 - S3 직접 조회 (cache 미호출)")
    fun `Non-cacheable 타입 - S3 직접 조회 (cache 미호출)`() {
        // Given: PHOTO_BOOTH는 cacheTtl=null이므로 isCacheable=false
        val ownerId = 1L
        val mediaId = 1L
        val storageKey = "photo-booth/test.jpg"
        val media =
            aMedia(id = mediaId, ownerId = ownerId, mediaType = MediaType.PHOTO_BOOTH, storageKey = storageKey)

        every { mediaRepository.getActiveMedias(ownerId, listOf(mediaId)) } returns listOf(media)
        every { mediaStorage.fetchBinaryByKey(storageKey) } returns imageData

        // When
        val command = GetMediasCommand(ownerId = ownerId, mediaIds = listOf(mediaId))
        val result = useCase.execute(command)

        // Then
        result.medias shouldHaveSize 1
        result.medias[0].binaryData shouldBe imageData
        verify(exactly = 0) { cache.get(any()) }
        verify(exactly = 0) { cache.put(any(), any(), any<Duration>()) }
    }

    @Test
    @DisplayName("빈 mediaIds - 빈 결과 반환")
    fun `빈 mediaIds - 빈 결과 반환`() {
        // Given
        val ownerId = 1L
        val mediaIds = emptyList<Long>()

        every { mediaRepository.getActiveMedias(ownerId, mediaIds) } returns emptyList()

        // When
        val command = GetMediasCommand(ownerId = ownerId, mediaIds = mediaIds)
        val result = useCase.execute(command)

        // Then
        result.medias shouldHaveSize 0
        verify(exactly = 0) { mediaStorage.fetchBinaryByKey(any()) }
        verify(exactly = 0) { cache.get(any()) }
    }

    @Test
    @DisplayName("부분 결과 - repository가 일부만 반환하면 반환된 것만 매핑")
    fun `부분 결과 - repository가 일부만 반환하면 반환된 것만 매핑`() {
        // Given: 5개 요청, 3개만 DB에 존재
        val ownerId = 1L
        val mediaIds = listOf(1L, 2L, 3L, 4L, 5L)
        val existingIds = listOf(1L, 3L, 5L)
        val medias = existingIds.map {
            aMedia(id = it, ownerId = ownerId, mediaType = MediaType.POSE, storageKey = "pose/test-$it.jpg")
        }

        every { mediaRepository.getActiveMedias(ownerId, mediaIds) } returns medias
        existingIds.forEach { id ->
            every { cache.get("pose/test-$id.jpg") } returns imageData
        }

        // When
        val command = GetMediasCommand(ownerId = ownerId, mediaIds = mediaIds)
        val result = useCase.execute(command)

        // Then
        result.medias shouldHaveSize 3
        result.medias.map { it.mediaId } shouldBe listOf(1L, 3L, 5L)
    }
}
