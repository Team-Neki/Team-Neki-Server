package com.yapp2app.media.infra.lock.redis

import com.yapp2app.media.application.port.DistributedLockPort
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Primary
import org.springframework.context.annotation.Profile
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.data.redis.core.script.DefaultRedisScript
import org.springframework.data.redis.core.script.RedisScript
import org.springframework.stereotype.Component
import java.time.Duration
import java.util.UUID
import kotlin.math.min

/**
 * Redis 기반 분산 락 어댑터.
 */
@Component
@Primary
@Profile("!test")
class RedisDistributedLockAdapter(private val redisTemplate: RedisTemplate<String, Any>) : DistributedLockPort {
    private val log = LoggerFactory.getLogger(javaClass)
    private val lockProperties = DistributedLockProperties.DEFAULT

    // 원자적 연산을 위한 lua script
    private val releaseLockScript: RedisScript<Long> =
        DefaultRedisScript(
            """
            if redis.call("get", KEYS[1]) == ARGV[1] then
                return redis.call("del", KEYS[1])
            else
                return 0
            end
            """.trimIndent(),
            Long::class.java,
        )

    /**
     * Generate Redis lock key from object key.
     * Format: lock:media:fetch:{objectKey}
     */
    private fun generateLockKey(objectKey: String): String = "lock:media:fetch:$objectKey"

    override fun <T> executeWithLock(key: String, ttl: Duration, action: () -> T): T? =
        executeWithRedisLock(key, ttl, action)

    private fun <T> executeWithRedisLock(key: String, ttl: Duration, action: () -> T): T? {
        val lockKey = generateLockKey(key) // Generate lock key internally
        val lockValue = UUID.randomUUID().toString()

        var delayMs = lockProperties.initialDelayMs

        repeat(lockProperties.maxRetries + 1) { attempt ->
            try {
                // Try to acquire Redis distributed lock
                val acquired =
                    redisTemplate.opsForValue()
                        .setIfAbsent(lockKey, lockValue, ttl) ?: false

                if (acquired) {
                    log.debug("[DistributedLock] Lock acquired for key: $key")
                    return try {
                        action()
                    } catch (e: Exception) {
                        log.error("[DistributedLock] Action failed for key: $key", e)
                        null
                    } finally {
                        releaseLock(lockKey, lockValue)
                    }
                }

                // Lock not acquired - wait and retry with exponential backoff
                if (attempt < lockProperties.maxRetries) {
                    log.debug(
                        "[DistributedLock] Waiting for lock holder (attempt ${attempt + 1}/${lockProperties.maxRetries}), key: $key",
                    )
                    Thread.sleep(delayMs)
                    delayMs = min(
                        delayMs * lockProperties.multiplier,
                        lockProperties.maxDelayMs.toDouble(),
                    ).toLong()
                }
            } catch (e: Exception) {
                log.warn("[DistributedLock] Redis operation failed (attempt ${attempt + 1}), key: $key", e)
                // Graceful degradation: continue to next retry or return null
            }
        }

        // All retries exhausted
        log.warn("[DistributedLock] Max retries exceeded, returning null for key: $key")
        return null
    }

    private fun releaseLock(lockKey: String, lockValue: String) {
        try {
            val released = redisTemplate.execute(releaseLockScript, listOf(lockKey), lockValue)
            if (released == 1L) {
                log.debug("[DistributedLock] Lock released for key: $lockKey")
            } else {
                log.debug("[DistributedLock] Lock already released or expired for key: $lockKey")
            }
        } catch (e: Exception) {
            log.warn("[DistributedLock] Failed to release lock for key: $lockKey", e)
            // Non-critical: Lock will auto-expire via TTL
        }
    }
}
