package com.neki.domain.media.service

import com.neki.domain.media.dto.MediaQuery
import com.neki.domain.media.external.DistributedLock
import com.neki.domain.media.external.MediaBinaryCache
import com.neki.domain.media.external.MediaStorage
import com.neki.domain.media.infra.metrics.MicrometerMediaBinaryCacheMetrics
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import java.time.Duration

/**
 * MediaBinaryService 의 PHOTO_BOOTH 캐시와 hit/miss 메트릭 테스트
 */
class MediaBinaryServiceTest :
    FunSpec({

        lateinit var cache: MediaBinaryCache
        lateinit var storage: MediaStorage
        lateinit var lock: DistributedLock
        lateinit var registry: SimpleMeterRegistry
        lateinit var service: MediaBinaryService

        val imageData = byteArrayOf(1, 2, 3)

        fun count(type: String, result: String): Double = registry.find(MicrometerMediaBinaryCacheMetrics.CACHE_METRIC)
            .tags("type", type, "result", result)
            .counter()
            ?.count() ?: 0.0

        fun query(key: String) = MediaQuery.GetImageByKey(objectKey = key)

        beforeTest {
            cache = mockk()
            storage = mockk()
            lock = mockk()
            registry = SimpleMeterRegistry()
            service = MediaBinaryService(cache, storage, lock, MicrometerMediaBinaryCacheMetrics(registry))
        }

        test("photo-booth 는 24시간 TTL 로 캐시에 저장하고 miss 를 센다") {
            val key = "photo-booth/a.jpg"
            every { cache.get(key) } returns null
            every { lock.executeWithLock<ByteArray>(key, any()) } answers { secondArg<() -> ByteArray>().invoke() }
            every { storage.fetchBinaryByKey(key) } returns imageData
            every { cache.put(key, imageData, Duration.ofHours(24)) } just Runs

            service.getBinaryByKey(query(key)) shouldBe imageData

            verify(exactly = 1) { cache.put(key, imageData, Duration.ofHours(24)) }
            count("photo-booth", MicrometerMediaBinaryCacheMetrics.RESULT_MISS) shouldBe 1.0
            count("photo-booth", MicrometerMediaBinaryCacheMetrics.RESULT_HIT) shouldBe 0.0
        }

        test("캐시 hit 이면 hit 를 한 번 센다") {
            val key = "photo-booth/a.jpg"
            every { cache.get(key) } returns imageData

            service.getBinaryByKey(query(key)) shouldBe imageData

            count("photo-booth", MicrometerMediaBinaryCacheMetrics.RESULT_HIT) shouldBe 1.0
            count("photo-booth", MicrometerMediaBinaryCacheMetrics.RESULT_MISS) shouldBe 0.0
            verify(exactly = 0) { storage.fetchBinaryByKey(any()) }
        }

        test("락을 못 잡고 재확인에서 찾은 경우도 요청당 miss 한 번만 센다") {
            val key = "pose/a.jpg"
            every { cache.get(key) } returnsMany listOf(null, imageData)
            every { lock.executeWithLock<ByteArray>(key, any()) } returns null

            service.getBinaryByKey(query(key)) shouldBe imageData

            count("pose", MicrometerMediaBinaryCacheMetrics.RESULT_MISS) shouldBe 1.0
            count("pose", MicrometerMediaBinaryCacheMetrics.RESULT_HIT) shouldBe 0.0
            verify(exactly = 0) { storage.fetchBinaryByKey(any()) }
        }

        test("캐시 비대상 타입은 캐시를 거치지 않고 메트릭도 남기지 않는다") {
            val key = "attachments/a.jpg"
            every { storage.fetchBinaryByKey(key) } returns imageData

            service.getBinaryByKey(query(key)) shouldBe imageData

            verify(exactly = 0) { cache.get(any()) }
            verify(exactly = 0) { lock.executeWithLock<Any>(any(), any()) }
            registry.find(MicrometerMediaBinaryCacheMetrics.CACHE_METRIC).counters().size shouldBe 0
        }
    })
