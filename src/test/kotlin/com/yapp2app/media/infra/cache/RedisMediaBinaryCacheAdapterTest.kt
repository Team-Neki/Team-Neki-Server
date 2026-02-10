package com.yapp2app.media.infra.cache

import com.yapp2app.media.application.port.MediaStoragePort
import com.yapp2app.media.infra.cache.redis.MediaRedisCacheKey
import com.yapp2app.media.infra.cache.redis.RedisMediaBinaryCacheAdapter
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.data.redis.core.ValueOperations
import java.time.Duration
import java.util.concurrent.Executor
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/**
 * fileName       : RedisMediaBinaryCacheAdapterTest
 * author         : koo
 * date           : 2026. 02. 08.
 * description    : RedisMediaBinaryCacheAdapter의 TTL 기반 lazy refresh 테스트
 */
class RedisMediaBinaryCacheAdapterTest :
    FunSpec({

        lateinit var mockRedisTemplate: RedisTemplate<String, ByteArray>
        lateinit var mockValueOps: ValueOperations<String, ByteArray>
        lateinit var mockMediaStorage: MediaStoragePort
        lateinit var executor: Executor
        lateinit var adapter: RedisMediaBinaryCacheAdapter

        beforeTest {
            mockRedisTemplate = mockk()
            mockValueOps = mockk()
            mockMediaStorage = mockk()
            executor = Executors.newFixedThreadPool(5)

            every { mockRedisTemplate.opsForValue() } returns mockValueOps

            adapter = RedisMediaBinaryCacheAdapter(mockRedisTemplate, mockMediaStorage, executor)
        }

        test("캐시 hit 시 TTL이 충분하면 async refresh를 트리거하지 않음") {
            // Given: TTL이 3시간 남음 (threshold 2시간보다 큼)
            val objectKey = "test/image.jpg"
            val cacheKey = MediaRedisCacheKey.binaryKey(objectKey)
            val cachedData = byteArrayOf(1, 2, 3)

            every { mockValueOps.get(cacheKey) } returns cachedData
            every { mockRedisTemplate.getExpire(cacheKey, TimeUnit.SECONDS) } returns 10800L // 3 hours

            // When
            val result = adapter.get(objectKey)

            // Then
            result shouldBe cachedData
            verify(exactly = 0) { mockMediaStorage.fetchBinaryByKey(any()) } // S3 fetch 없음
        }

        test("캐시 hit 시 TTL이 threshold 미만이면 async refresh 트리거") {
            // Given: TTL이 1시간 남음 (threshold 2시간보다 작음)
            val objectKey = "test/image.jpg"
            val cacheKey = MediaRedisCacheKey.binaryKey(objectKey)
            val cachedData = byteArrayOf(1, 2, 3)
            val freshData = byteArrayOf(4, 5, 6)

            every { mockValueOps.get(cacheKey) } returns cachedData
            every { mockRedisTemplate.getExpire(cacheKey, TimeUnit.SECONDS) } returns 3600L // 1 hour
            every { mockMediaStorage.fetchBinaryByKey(objectKey) } returns freshData
            every { mockValueOps.set(cacheKey, freshData, any<Duration>()) } just Runs

            // When
            val result = adapter.get(objectKey)

            // Then
            result shouldBe cachedData // 즉시 기존 캐시 반환

            // Wait for async refresh to complete
            Thread.sleep(200)

            verify(exactly = 1) { mockMediaStorage.fetchBinaryByKey(objectKey) }
            verify(exactly = 1) { mockValueOps.set(cacheKey, freshData, any<Duration>()) }
        }

        test("TTL check 실패 시 graceful degradation") {
            // Given
            val objectKey = "test/image.jpg"
            val cacheKey = MediaRedisCacheKey.binaryKey(objectKey)
            val cachedData = byteArrayOf(1, 2, 3)

            every { mockValueOps.get(cacheKey) } returns cachedData
            every { mockRedisTemplate.getExpire(cacheKey, TimeUnit.SECONDS) } throws RuntimeException("Redis error")

            // When
            val result = adapter.get(objectKey)

            // Then
            result shouldBe cachedData // 여전히 캐시된 데이터 반환
            verify(exactly = 0) { mockMediaStorage.fetchBinaryByKey(any()) }
        }

        test("async refresh 실패 시 예외를 전파하지 않음") {
            // Given
            val objectKey = "test/image.jpg"
            val cacheKey = MediaRedisCacheKey.binaryKey(objectKey)
            val cachedData = byteArrayOf(1, 2, 3)

            every { mockValueOps.get(cacheKey) } returns cachedData
            every { mockRedisTemplate.getExpire(cacheKey, TimeUnit.SECONDS) } returns 3600L
            every { mockMediaStorage.fetchBinaryByKey(objectKey) } throws RuntimeException("S3 error")

            // When
            val result = adapter.get(objectKey)

            // Then
            result shouldBe cachedData // 원래 요청은 성공

            Thread.sleep(200)
            // Async refresh 실패했지만 예외가 전파되지 않음
        }

        test("캐시 miss 시 async refresh 트리거하지 않음") {
            // Given
            val objectKey = "test/image.jpg"
            val cacheKey = MediaRedisCacheKey.binaryKey(objectKey)

            every { mockValueOps.get(cacheKey) } returns null

            // When
            val result = adapter.get(objectKey)

            // Then
            result shouldBe null
            verify(exactly = 0) { mockRedisTemplate.getExpire(any(), any()) } // TTL check 없음
            verify(exactly = 0) { mockMediaStorage.fetchBinaryByKey(any()) }
        }

        test("TTL이 0 또는 음수이면 async refresh 트리거하지 않음 (만료된 캐시)") {
            // Given
            val objectKey = "test/image.jpg"
            val cacheKey = MediaRedisCacheKey.binaryKey(objectKey)
            val cachedData = byteArrayOf(1, 2, 3)

            every { mockValueOps.get(cacheKey) } returns cachedData
            every { mockRedisTemplate.getExpire(cacheKey, TimeUnit.SECONDS) } returns -1L // 만료됨

            // When
            val result = adapter.get(objectKey)

            // Then
            result shouldBe cachedData
            verify(exactly = 0) { mockMediaStorage.fetchBinaryByKey(any()) }
        }

        test("put 메서드는 24시간 TTL로 캐시 저장") {
            // Given
            val objectKey = "test/image.jpg"
            val cacheKey = MediaRedisCacheKey.binaryKey(objectKey)
            val data = byteArrayOf(1, 2, 3)

            every { mockValueOps.set(cacheKey, data, Duration.ofHours(24)) } just Runs

            // When
            adapter.put(objectKey, data)

            // Then
            verify(exactly = 1) { mockValueOps.set(cacheKey, data, Duration.ofHours(24)) }
        }

        test("evict 메서드는 캐시 삭제") {
            // Given
            val objectKey = "test/image.jpg"
            val cacheKey = MediaRedisCacheKey.binaryKey(objectKey)

            every { mockRedisTemplate.delete(cacheKey) } returns true

            // When
            adapter.evict(objectKey)

            // Then
            verify(exactly = 1) { mockRedisTemplate.delete(cacheKey) }
        }
    })
