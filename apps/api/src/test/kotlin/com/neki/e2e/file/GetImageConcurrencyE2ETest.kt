package com.neki.e2e.file

import com.neki.e2e.E2ETestBase
import com.neki.media.application.port.MediaBinaryCachePort
import com.neki.media.application.port.MediaStoragePort
import com.neki.media.infra.storage.fake.FakeMediaStorageAdapter
import io.restassured.RestAssured
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.http.HttpStatus
import org.springframework.test.context.ActiveProfiles
import java.time.Duration
import java.util.concurrent.CompletableFuture

/**
 * fileName       : GetImageConcurrencyE2ETest
 * author         : koo
 * date           : 2026. 02. 08.
 * description    : 분산 락을 통한 동시 요청 중복 제거 검증 E2E 테스트
 *
 * 목적: Cache stampede 방지 검증
 * - 동시에 여러 요청이 들어올 때 S3 중복 호출 방지
 * - 분산 락을 통해 첫 번째 요청만 S3 조회, 나머지는 캐시에서 반환
 */
@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class GetImageConcurrencyE2ETest : E2ETestBase() {

    @LocalServerPort
    private var port: Int = 0

    @Autowired
    private lateinit var mediaStoragePort: MediaStoragePort

    @Autowired
    private lateinit var cache: MediaBinaryCachePort

    private val fakeStorage: FakeMediaStorageAdapter
        get() = mediaStoragePort as FakeMediaStorageAdapter

    @BeforeEach
    fun setUp() {
        RestAssured.port = port
        RestAssured.baseURI = "http://localhost"
    }

    @AfterEach
    override fun tearDown() {
        // Clear cache for all test keys to ensure test isolation
        listOf(
            "pose/concurrent-image.jpg",
            "pose/cached-image.jpg",
            "pose/image1.jpg",
            "pose/image2.jpg",
            "pose/image3.jpg",
        ).forEach { cache.evict(it) }

        fakeStorage.clearTestData()
        super.tearDown()
    }

    @Test
    @DisplayName("동시 요청 시 S3 중복 호출 방지 - 분산 락 적용")
    fun givenConcurrentRequests_whenGetSameImage_thenSingleS3Fetch() {
        // Given: 테스트 이미지 준비
        val objectKey = "pose/concurrent-image.jpg"
        val imageData = createTestJpegData()
        fakeStorage.putTestData(objectKey, imageData)

        // When: 20개의 동시 요청
        val futures = (1..20).map {
            CompletableFuture.supplyAsync {
                RestAssured.given()
                    .`when`()
                    .get("/file/image/$objectKey")
                    .then()
                    .statusCode(HttpStatus.OK.value())
                    .extract()
                    .asByteArray()
            }
        }

        val results = futures.map { it.get() }

        // Then: 모든 요청 성공
        assertEquals(20, results.size, "All 20 requests should succeed")
        results.forEach { assertNotNull(it, "Each response should contain data") }

        // Then: S3 fetch는 1-3회만 발생 (이상적으로는 1회, 타이밍 이슈로 2-3회 허용)
        val fetchCount = fakeStorage.getFetchCount(objectKey)
        assertTrue(
            fetchCount <= 3,
            "S3 fetch count should be ≤ 3 (actual: $fetchCount). " +
                "Distributed lock should prevent duplicate fetches.",
        )

        println("[ConcurrencyTest] 20 concurrent requests → $fetchCount S3 fetch(es)")
    }

    @Test
    @DisplayName("캐시 hit 시 락 없이 즉시 반환")
    fun givenCachedImage_whenConcurrentRequests_thenNoLockAcquisition() {
        // Given: 캐시에 이미지 미리 저장
        val objectKey = "pose/cached-image.jpg"
        val imageData = createTestJpegData()
        fakeStorage.putTestData(objectKey, imageData)
        cache.put(objectKey, imageData, Duration.ofHours(24))

        // When: 10개의 동시 요청
        val futures = (1..10).map {
            CompletableFuture.supplyAsync {
                RestAssured.given()
                    .`when`()
                    .get("/file/image/$objectKey")
                    .then()
                    .statusCode(HttpStatus.OK.value())
                    .extract()
                    .asByteArray()
            }
        }

        val results = futures.map { it.get() }

        // Then: 모든 요청 성공
        assertEquals(10, results.size)
        results.forEach { assertNotNull(it) }

        // Then: S3 fetch 없음 (캐시에서만 반환)
        val fetchCount = fakeStorage.getFetchCount(objectKey)
        assertEquals(
            0,
            fetchCount,
            "S3 fetch should not occur when cache hit. Actual: $fetchCount",
        )

        println("[ConcurrencyTest] 10 cache hit requests → 0 S3 fetch")
    }

    @Test
    @DisplayName("서로 다른 이미지 동시 요청 - 각각 독립적으로 처리")
    fun givenDifferentImages_whenConcurrentRequests_thenEachFetchedIndependently() {
        // Given: 3개의 서로 다른 이미지
        val objectKeys = listOf(
            "pose/image1.jpg",
            "pose/image2.jpg",
            "pose/image3.jpg",
        )
        objectKeys.forEach { fakeStorage.putTestData(it, createTestJpegData()) }

        // When: 각 이미지당 5개씩 동시 요청 (총 15개)
        val futures = objectKeys.flatMap { objectKey ->
            (1..5).map {
                CompletableFuture.supplyAsync {
                    RestAssured.given()
                        .`when`()
                        .get("/file/image/$objectKey")
                        .then()
                        .statusCode(HttpStatus.OK.value())
                        .extract()
                        .asByteArray()
                }
            }
        }

        val results = futures.map { it.get() }

        // Then: 모든 요청 성공
        assertEquals(15, results.size)

        // Then: 각 이미지는 최대 3회 fetch (ideally 1회)
        objectKeys.forEach { objectKey ->
            val fetchCount = fakeStorage.getFetchCount(objectKey)
            assertTrue(
                fetchCount <= 3,
                "Each image should be fetched ≤ 3 times. $objectKey: $fetchCount",
            )
        }

        // Then: 총 S3 fetch는 3-9회 (이미지당 1-3회)
        val totalFetchCount = fakeStorage.getTotalFetchCount()
        assertTrue(
            totalFetchCount in 3..9,
            "Total S3 fetch should be 3-9. Actual: $totalFetchCount",
        )

        println("[ConcurrencyTest] 15 requests (3 images × 5) → $totalFetchCount S3 fetch(es)")
    }

    private fun createTestJpegData(): ByteArray = byteArrayOf(
        0xFF.toByte(),
        0xD8.toByte(),
        0xFF.toByte(),
        0xE0.toByte(),
        0x00,
        0x10,
        0x4A,
        0x46,
        0x49,
        0x46,
        0x00,
        0x01,
        0x02,
        0x03,
        0x04,
        0x05,
    )
}
