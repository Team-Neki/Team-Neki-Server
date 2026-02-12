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
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.min

/**
 * Redis 기반 분산 락 어댑터.
 *
 * 2단계 중복 제거 전략:
 * 1. In-Memory Single-Flight: 동일 JVM 내 동시 요청 중복 제거 (ConcurrentHashMap)
 * 2. Redis Distributed Lock: 여러 Pod 간 중복 제거 (SET key NX PX)
 *
 * 특징:
 * - 락 타임아웃: TTL로 자동 해제 (holder 크래시 방지)
 * - 재시도 전략: 지수 백오프 + 캐시 체크
 * - Graceful Degradation: Redis 장애 시 락 없이 진행
 * - 원자적 락 해제: Lua 스크립트로 다른 프로세스 락 보호
 */
@Component
@Primary
@Profile("!test")
class RedisDistributedLockAdapter(private val redisTemplate: RedisTemplate<String, Any>) : DistributedLockPort {
    private val log = LoggerFactory.getLogger(javaClass)

    // In-memory single-flight map: 동일 JVM 내 동시 요청 중복 제거
    private val inFlightOperations = ConcurrentHashMap<String, CompletableFuture<Any?>>()

    companion object {
        private val DEFAULT_RETRY_CONFIG =
            LockRetryConfig(
                maxRetries = 5,
                initialDelayMs = 50,
                maxDelayMs = 500,
                multiplier = 1.5,
            )
    }

    /**
     * 락 재시도 설정
     *
     * Redis 특성과 네트워크 환경에 맞춰 조정된 재시도 정책입니다.
     */
    private data class LockRetryConfig(
        val maxRetries: Int,
        val initialDelayMs: Long,
        val maxDelayMs: Long,
        val multiplier: Double,
    )

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

    override fun <T> executeWithLock(key: String, ttl: Duration, action: () -> T): T? {
        val newFuture = CompletableFuture<Any?>()
        val existingFuture = inFlightOperations.putIfAbsent(key, newFuture)

        if (existingFuture != null) {
            // 다른 스레드가 이미 실행 중 → 결과 대기
            return try {
                @Suppress("UNCHECKED_CAST")
                existingFuture.get() as? T
            } catch (e: Exception) {
                log.warn("[DistributedLock] In-flight operation failed for key: $key", e)
                null
            } finally {
                inFlightOperations.remove(key, existingFuture)
            }
        }

        // 첫 번째 스레드 → 호출 스레드(Tomcat)에서 직접 실행
        return try {
            val result = executeWithRedisLock(key, ttl, DEFAULT_RETRY_CONFIG, action)
            newFuture.complete(result)
            @Suppress("UNCHECKED_CAST")
            result as? T
        } catch (e: Exception) {
            newFuture.completeExceptionally(e)
            null
        } finally {
            inFlightOperations.remove(key, newFuture)
        }
    }

    private fun <T> executeWithRedisLock(
        key: String,
        ttl: Duration,
        retryConfig: LockRetryConfig,
        action: () -> T,
    ): T? {
        val lockKey = generateLockKey(key) // Generate lock key internally
        val lockValue = UUID.randomUUID().toString()

        var delayMs = retryConfig.initialDelayMs

        repeat(retryConfig.maxRetries + 1) { attempt ->
            try {
                // 2. Try to acquire Redis distributed lock
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

                // 3. Lock not acquired - wait and retry with exponential backoff
                if (attempt < retryConfig.maxRetries) {
                    log.debug(
                        "[DistributedLock] Waiting for lock holder (attempt ${attempt + 1}/${retryConfig.maxRetries}), key: $key",
                    )
                    Thread.sleep(delayMs)
                    delayMs = min(
                        delayMs * retryConfig.multiplier,
                        retryConfig.maxDelayMs.toDouble(),
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
