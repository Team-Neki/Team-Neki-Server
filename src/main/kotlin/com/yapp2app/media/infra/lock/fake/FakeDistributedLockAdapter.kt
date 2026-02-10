package com.yapp2app.media.infra.lock.fake

import com.yapp2app.media.application.port.DistributedLockPort
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component
import java.time.Duration
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executor

/**
 * 테스트용 In-Memory 분산 락 어댑터.
 *
 * Redis 의존성 없이 단일 JVM 내에서 락 동작을 시뮬레이션합니다.
 * 프로덕션 어댑터와 동일한 single-flight 패턴을 사용하여
 * 테스트 환경에서도 동시성 제어를 검증할 수 있습니다.
 */
@Component
@Profile("test")
class FakeDistributedLockAdapter(@Qualifier("asyncExecutor") private val executor: Executor) : DistributedLockPort {
    private val log = LoggerFactory.getLogger(javaClass)

    // In-memory single-flight map (same as production)
    private val inFlightOperations = ConcurrentHashMap<String, CompletableFuture<Any?>>()

    // Test helper: Track held locks
    private val heldLocks = ConcurrentHashMap.newKeySet<String>()

    /**
     * Generate lock key from object key.
     * Format: lock:media:fetch:{objectKey}
     */
    private fun generateLockKey(objectKey: String): String = "lock:media:fetch:$objectKey"

    override fun <T> executeWithLock(key: String, ttl: Duration, action: () -> T): T? {
        val future =
            inFlightOperations.computeIfAbsent(key) {
                CompletableFuture.supplyAsync(
                    { executeAction(key, action) },
                    executor,
                )
            }

        return try {
            @Suppress("UNCHECKED_CAST")
            future.get() as? T
        } finally {
            inFlightOperations.remove(key, future)
        }
    }

    private fun <T> executeAction(key: String, action: () -> T): T? {
        val lockKey = generateLockKey(key)
        return try {
            heldLocks.add(lockKey)
            log.debug("[FakeDistributedLock] Lock acquired for key: $lockKey")
            action()
        } catch (e: Exception) {
            log.error("[FakeDistributedLock] Action failed for key: $key", e)
            null
        } finally {
            heldLocks.remove(lockKey)
            log.debug("[FakeDistributedLock] Lock released for key: $lockKey")
        }
    }

    // Test helpers
    fun isLockHeld(key: String): Boolean = heldLocks.contains(key)

    fun clearAll() {
        inFlightOperations.clear()
        heldLocks.clear()
    }
}
