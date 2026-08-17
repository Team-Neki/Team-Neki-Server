package com.neki.api.media.application.usecase

import com.neki.api.media.application.GetImageByKeyUseCase
import com.neki.domain.media.dto.MediaQuery
import com.neki.domain.media.external.DistributedLock
import com.neki.domain.media.external.MediaBinaryCache
import com.neki.domain.media.external.MediaStorage
import com.neki.domain.media.service.MediaBinaryService
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.time.Duration

/**
 * fileName       : GetImageByKeyUseCaseTest
 * description    : GetImageByKeyUseCase 단위 테스트
 */
class GetImageByKeyUseCaseTest {

    private lateinit var mediaStorage: MediaStorage
    private lateinit var cache: MediaBinaryCache
    private lateinit var distributedLock: DistributedLock
    private lateinit var useCase: GetImageByKeyUseCase

    private val imageData = byteArrayOf(1, 2, 3, 4)

    @BeforeEach
    fun setUp() {
        mediaStorage = mockk()
        cache = mockk()
        distributedLock = mockk()
        useCase = GetImageByKeyUseCase(MediaBinaryService(cache, mediaStorage, distributedLock))
    }

    @Test
    @DisplayName("캐싱 비대상 타입 - S3 직접 조회 (lock/cache 미호출)")
    fun `캐싱 비대상 타입 - S3 직접 조회 (lock_cache 미호출)`() {
        // Given: photo-booth는 cacheTtl=null 이므로 isCacheable=false
        val objectKey = "photo-booth/image.jpg"

        every { mediaStorage.fetchBinaryByKey(objectKey) } returns imageData

        // When
        val result = useCase.execute(MediaQuery.GetImageByKey(objectKey = objectKey))

        // Then
        result.binaryData shouldBe imageData
        result.contentType shouldBe "image/jpeg"
        verify(exactly = 0) { cache.get(any()) }
        verify(exactly = 0) { distributedLock.executeWithLock<Any>(any(), any()) }
    }

    @Test
    @DisplayName("Cache hit - 즉시 반환 (S3 미호출)")
    fun `Cache hit - 즉시 반환 (S3 미호출)`() {
        // Given: pose는 cacheable
        val objectKey = "pose/image.jpg"

        every { cache.get(objectKey) } returns imageData

        // When
        val result = useCase.execute(MediaQuery.GetImageByKey(objectKey = objectKey))

        // Then
        result.binaryData shouldBe imageData
        result.contentType shouldBe "image/jpeg"
        verify(exactly = 0) { mediaStorage.fetchBinaryByKey(any()) }
        verify(exactly = 0) { distributedLock.executeWithLock<Any>(any(), any()) }
    }

    @Test
    @DisplayName("Cache miss + lock 획득 - S3 조회 후 캐싱하여 반환")
    fun `Cache miss + lock 획득 - S3 조회 후 캐싱하여 반환`() {
        // Given
        val objectKey = "pose/image.jpg"

        every { cache.get(objectKey) } returnsMany listOf(null, null) // 첫 번째 캐시 miss, lock 내부 double-check도 miss
        every { mediaStorage.fetchBinaryByKey(objectKey) } returns imageData
        every { cache.put(objectKey, imageData, any<Duration>()) } returns Unit
        every {
            distributedLock.executeWithLock<ByteArray>(objectKey, any())
        } answers {
            val action = secondArg<() -> ByteArray>()
            action()
        }

        // When
        val result = useCase.execute(MediaQuery.GetImageByKey(objectKey = objectKey))

        // Then
        result.binaryData shouldBe imageData
        verify(exactly = 1) { mediaStorage.fetchBinaryByKey(objectKey) }
        verify(exactly = 1) { cache.put(objectKey, imageData, any<Duration>()) }
    }

    @Test
    @DisplayName("Cache miss + lock 미획득 - 캐시 재확인 후 반환")
    fun `Cache miss + lock 미획득 - 캐시 재확인 후 반환`() {
        // Given: lock 획득 실패(null 반환), 이후 캐시에서 data 발견
        val objectKey = "pose/image.jpg"

        every { cache.get(objectKey) } returnsMany listOf(null, imageData) // 첫 번째 miss, 두 번째 hit
        every {
            distributedLock.executeWithLock<ByteArray>(objectKey, any())
        } returns null // 락 미획득

        // When
        val result = useCase.execute(MediaQuery.GetImageByKey(objectKey = objectKey))

        // Then
        result.binaryData shouldBe imageData
        verify(exactly = 0) { mediaStorage.fetchBinaryByKey(any()) }
    }

    @Test
    @DisplayName("Cache miss + lock 미획득 + 캐시도 비어있음 - S3 직접 조회로 폴백")
    fun `Cache miss + lock 미획득 + 캐시도 비어있음 - S3 직접 조회로 폴백`() {
        // Given
        val objectKey = "pose/image.jpg"

        every { cache.get(objectKey) } returns null // 항상 miss
        every { mediaStorage.fetchBinaryByKey(objectKey) } returns imageData
        every {
            distributedLock.executeWithLock<ByteArray>(objectKey, any())
        } returns null // 락 미획득

        // When
        val result = useCase.execute(MediaQuery.GetImageByKey(objectKey = objectKey))

        // Then: 캐시를 채우지는 않고 S3 결과를 그대로 반환
        result.binaryData shouldBe imageData
        verify(exactly = 1) { mediaStorage.fetchBinaryByKey(objectKey) }
        verify(exactly = 0) { cache.put(any(), any(), any<Duration>()) }
    }

    @Test
    @DisplayName("Content-Type 매핑 - jpg는 image/jpeg, unknown 확장자는 application/octet-stream")
    fun `Content-Type 매핑 - jpg는 image_jpeg, unknown 확장자는 application_octet-stream`() {
        // Given
        val jpgKey = "pose/photo.jpg"
        val unknownKey = "pose/photo.xyz"

        every { cache.get(jpgKey) } returns imageData
        every { cache.get(unknownKey) } returns imageData

        // When
        val jpgResult = useCase.execute(MediaQuery.GetImageByKey(objectKey = jpgKey))
        val unknownResult = useCase.execute(MediaQuery.GetImageByKey(objectKey = unknownKey))

        // Then
        jpgResult.contentType shouldBe "image/jpeg"
        unknownResult.contentType shouldBe "application/octet-stream"
    }

    @Test
    @DisplayName("lock 내부 double-check - lock 획득 후 캐시 재확인 hit이면 S3 미호출")
    fun `lock 내부 double-check - lock 획득 후 캐시 재확인 hit이면 S3 미호출`() {
        // Given
        val objectKey = "pose/image.jpg"

        every { cache.get(objectKey) } returnsMany listOf(null, imageData) // 첫 번째 miss, lock 내부에서 hit
        every {
            distributedLock.executeWithLock<ByteArray>(objectKey, any())
        } answers {
            val action = secondArg<() -> ByteArray>()
            action()
        }

        // When
        val result = useCase.execute(MediaQuery.GetImageByKey(objectKey = objectKey))

        // Then
        result.binaryData shouldBe imageData
        verify(exactly = 0) { mediaStorage.fetchBinaryByKey(any()) }
        verify(exactly = 0) { cache.put(any(), any(), any<Duration>()) }
    }

    @Test
    @DisplayName("확장자 없는 objectKey - DEFAULT_CONTENT_TYPE(application/octet-stream) 반환")
    fun `확장자 없는 objectKey - DEFAULT_CONTENT_TYPE(application_octet-stream) 반환`() {
        // Given: 접두사는 pose(cacheable), 파일명은 확장자 없음
        val objectKey = "pose/image"

        every { cache.get(objectKey) } returns imageData

        // When
        val result = useCase.execute(MediaQuery.GetImageByKey(objectKey = objectKey))

        // Then
        result.contentType shouldBe "application/octet-stream"
    }
}
