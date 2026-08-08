package com.neki.api.media.infra.lock.fake

import com.neki.domain.media.external.DistributedLock
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.locks.ReentrantLock

/**
 * 테스트용 In-Memory 분산 락 어댑터.
 *
 * Redis 의존성 없이 단일 JVM 내에서 락 동작을 시뮬레이션합니다.
 */
@Component
@Profile("test")
class FakeDistributedLockAdapter : DistributedLock {
    private val log = LoggerFactory.getLogger(javaClass)

    // Per-key locks for mutual exclusion
    private val keyLocks = ConcurrentHashMap<String, ReentrantLock>()

    // Test helper: Track held locks
    private val heldLocks = ConcurrentHashMap.newKeySet<String>()

    /**
     * Generate lock key from object key.
     * Format: lock:media:fetch:{objectKey}
     */
    private fun generateLockKey(objectKey: String): String = "lock:media:fetch:$objectKey"

    override fun <T> executeWithLock(key: String, action: () -> T): T? {
        val lockKey = generateLockKey(key)
        val lock = keyLocks.computeIfAbsent(lockKey) { ReentrantLock() }
        lock.lock()
        return try {
            heldLocks.add(lockKey)
            log.debug("[FakeDistributedLock] Lock acquired for key: $lockKey")
            action()
        } catch (e: Exception) {
            log.error("[FakeDistributedLock] Action failed for key: $key", e)
            null
        } finally {
            heldLocks.remove(lockKey)
            lock.unlock()
            log.debug("[FakeDistributedLock] Lock released for key: $lockKey")
        }
    }

    // Test helpers
    fun isLockHeld(key: String): Boolean = heldLocks.contains(key)

    fun clearAll() {
        heldLocks.clear()
        keyLocks.clear()
    }
}
