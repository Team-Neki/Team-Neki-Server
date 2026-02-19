package com.neki.media.infra.lock

import com.neki.media.infra.lock.redis.RedisDistributedLockAdapter
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.data.redis.core.ValueOperations
import org.springframework.data.redis.core.script.RedisScript
import java.time.Duration
import java.util.concurrent.CompletableFuture
import java.util.concurrent.atomic.AtomicInteger

class RedisDistributedLockAdapterTest :
    FunSpec({

        lateinit var mockRedisTemplate: RedisTemplate<String, Any>
        lateinit var mockValueOps: ValueOperations<String, Any>
        lateinit var adapter: RedisDistributedLockAdapter

        beforeTest {
            mockRedisTemplate = mockk()
            mockValueOps = mockk()
            every { mockRedisTemplate.opsForValue() } returns mockValueOps
            adapter = RedisDistributedLockAdapter(mockRedisTemplate)
        }

        test("Lock acquired successfully - action should execute") {
            // Given
            val objectKey = "test/image.jpg"
            val lockKey = "lock:media:fetch:$objectKey"
            val actionExecuted = AtomicInteger(0)

            every {
                mockValueOps.setIfAbsent(lockKey, any(), any<Duration>())
            } returns true

            every {
                mockRedisTemplate.execute(any<RedisScript<Long>>(), any<List<String>>(), any())
            } returns 1L

            // When
            val result =
                adapter.executeWithLock(
                    key = objectKey,
                    ttl = Duration.ofSeconds(10),
                ) {
                    actionExecuted.incrementAndGet()
                    "result"
                }

            // Then
            result shouldBe "result"
            actionExecuted.get() shouldBe 1
            verify(exactly = 1) { mockValueOps.setIfAbsent(lockKey, any(), any<Duration>()) }
            verify(exactly = 1) {
                mockRedisTemplate.execute(
                    any<RedisScript<Long>>(),
                    any<List<String>>(),
                    any(),
                )
            }
        }

        test("Lock not acquired - should retry with exponential backoff") {
            // Given: Default retry config (maxRetries = 5)
            val objectKey = "test/image.jpg"
            val lockKey = "lock:media:fetch:$objectKey"
            var attemptCount = 0

            every {
                mockValueOps.setIfAbsent(lockKey, any(), any<Duration>())
            } answers {
                attemptCount++
                false // Lock never acquired
            }

            // When
            val result =
                adapter.executeWithLock(
                    key = objectKey,
                    ttl = Duration.ofSeconds(10),
                ) {
                    "should not execute"
                }

            // Then
            result shouldBe null // All retries exhausted
            attemptCount shouldBe 6 // Initial + 5 retries (default)
        }

        test("Redis exception - should return null (graceful degradation)") {
            // Given
            val objectKey = "test/image.jpg"
            val lockKey = "lock:media:fetch:$objectKey"

            every {
                mockValueOps.setIfAbsent(lockKey, any(), any<Duration>())
            } throws RuntimeException("Redis connection failed")

            // When
            val result =
                adapter.executeWithLock(
                    key = objectKey,
                    ttl = Duration.ofSeconds(10),
                ) {
                    "should not execute"
                }

            // Then
            result shouldBe null // Graceful degradation
        }

        test("Action throws exception - should return null and release lock") {
            // Given
            val objectKey = "test/image.jpg"
            val lockKey = "lock:media:fetch:$objectKey"

            every {
                mockValueOps.setIfAbsent(lockKey, any(), any<Duration>())
            } returns true

            every {
                mockRedisTemplate.execute(any<RedisScript<Long>>(), any<List<String>>(), any())
            } returns 1L

            // When
            val result =
                adapter.executeWithLock(
                    key = objectKey,
                    ttl = Duration.ofSeconds(10),
                ) {
                    throw RuntimeException("Action failed")
                }

            // Then
            result shouldBe null // Exception caught
            verify(exactly = 1) {
                mockRedisTemplate.execute(
                    any<RedisScript<Long>>(),
                    any<List<String>>(),
                    any(),
                )
            } // Lock still released
        }

        test("Lock release failure - should not throw exception") {
            // Given
            val objectKey = "test/image.jpg"
            val lockKey = "lock:media:fetch:$objectKey"

            every {
                mockValueOps.setIfAbsent(lockKey, any(), any<Duration>())
            } returns true

            every {
                mockRedisTemplate.execute(any<RedisScript<Long>>(), any<List<String>>(), any())
            } throws RuntimeException("Redis connection lost")

            // When/Then: Should not throw
            val result =
                adapter.executeWithLock(
                    key = objectKey,
                    ttl = Duration.ofSeconds(10),
                ) {
                    "result"
                }

            result shouldBe "result" // Action still executed
        }

        test("Different keys should not interfere with each other") {
            // Given
            val objectKey1 = "test/image1.jpg"
            val objectKey2 = "test/image2.jpg"
            val executions = AtomicInteger(0)

            every {
                mockValueOps.setIfAbsent(any(), any(), any<Duration>())
            } returns true

            every {
                mockRedisTemplate.execute(any<RedisScript<Long>>(), any<List<String>>(), any())
            } returns 1L

            // When: Concurrent requests for different keys
            val future1 =
                CompletableFuture.supplyAsync {
                    adapter.executeWithLock(
                        key = objectKey1,
                        ttl = Duration.ofSeconds(10),
                    ) {
                        executions.incrementAndGet()
                        "result1"
                    }
                }

            val future2 =
                CompletableFuture.supplyAsync {
                    adapter.executeWithLock(
                        key = objectKey2,
                        ttl = Duration.ofSeconds(10),
                    ) {
                        executions.incrementAndGet()
                        "result2"
                    }
                }

            val results = listOf(future1.get(), future2.get())

            // Then: Both should execute (different keys)
            results.size shouldBe 2
            executions.get() shouldBe 2
        }
    })
