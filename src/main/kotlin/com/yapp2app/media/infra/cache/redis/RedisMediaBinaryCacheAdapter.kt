package com.yapp2app.media.infra.cache.redis

import com.yapp2app.media.application.port.MediaBinaryCachePort
import com.yapp2app.media.application.port.MediaStoragePort
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.annotation.Primary
import org.springframework.context.annotation.Profile
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Component
import java.time.Duration
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executor
import java.util.concurrent.TimeUnit

/**
 * fileName       : RedisMediaBinaryCacheAdapter
 * author         : koo
 * date           : 2026. 02. 08.
 * description    : Redis를 사용한 이미지 바이너리 캐싱 구현
 *
 * - S3 API 호출 횟수 감소를 통한 비용 절감
 * - 응답 속도 향상으로 사용자 경험 개선
 * - Redis 장애 시에도 graceful degradation 보장 (S3 fallback)
 * - 24시간 TTL로 메모리 효율성과 캐시 효율성 균형
 * - TTL 기반 lazy refresh로 인기 콘텐츠의 캐시 만료 방지
 */
@Component
@Primary
@Profile("!test")
class RedisMediaBinaryCacheAdapter(
    private val binaryRedisTemplate: RedisTemplate<String, ByteArray>,
    private val mediaStorage: MediaStoragePort,
    @Qualifier("asyncExecutor") private val executor: Executor,
) : MediaBinaryCachePort {

    private val log = LoggerFactory.getLogger(javaClass)

    companion object {
        private val DEFAULT_TTL = Duration.ofHours(24)
        private val REFRESH_THRESHOLD = Duration.ofHours(2) // TTL이 2시간 미만 남았을 때 갱신
    }

    override fun get(key: String): ByteArray? {
        val cacheKey = MediaRedisCacheKey.binaryKey(key)
        return try {
            val cached = binaryRedisTemplate.opsForValue().get(cacheKey)
            if (cached != null) {
                log.debug("[MediaCache] Cache hit for key: $key")

                // TTL 확인 후 갱신 필요 시 비동기 refresh 트리거
                checkAndRefreshIfNeeded(key, cacheKey)

                cached
            } else {
                log.debug("[MediaCache] Cache miss for key: $key")
                null
            }
        } catch (e: Exception) {
            log.warn("[MediaCache] Cache get failed for key: $key", e)
            null // graceful degradation: cache miss로 처리하여 S3에서 조회
        }
    }

    override fun put(key: String, value: ByteArray) {
        val cacheKey = MediaRedisCacheKey.binaryKey(key)
        try {
            binaryRedisTemplate.opsForValue().set(cacheKey, value, DEFAULT_TTL)
            log.debug("[MediaCache] Cache put successful for key: $key (TTL: $DEFAULT_TTL)")
        } catch (e: Exception) {
            log.error("[MediaCache] Cache put failed for key: $key", e)
            // 예외를 던지지 않음 - 캐싱 실패해도 서비스는 계속 동작
        }
    }

    override fun evict(key: String) {
        val cacheKey = MediaRedisCacheKey.binaryKey(key)
        try {
            val deleted = binaryRedisTemplate.delete(cacheKey)
            if (deleted) {
                log.debug("[MediaCache] Cache evict successful for key: $key")
            } else {
                log.debug("[MediaCache] Cache evict skipped (key not found): $key")
            }
        } catch (e: Exception) {
            log.error("[MediaCache] Cache evict failed for key: $key", e)
            // 예외를 던지지 않음 - 캐시 무효화 실패해도 서비스는 계속 동작
        }
    }

    /**
     * TTL을 확인하고 임계값 미만이면 비동기 갱신을 트리거
     */
    private fun checkAndRefreshIfNeeded(objectKey: String, cacheKey: String) {
        try {
            val ttlSeconds = binaryRedisTemplate.getExpire(cacheKey, TimeUnit.SECONDS)

            if (ttlSeconds > 0 && ttlSeconds < REFRESH_THRESHOLD.seconds) {
                log.debug(
                    "[MediaCache] TTL low ($ttlSeconds seconds remaining), triggering async refresh for key: $objectKey",
                )
                triggerAsyncRefresh(objectKey)
            }
        } catch (e: Exception) {
            log.warn("[MediaCache] TTL check failed for key: $objectKey", e)
            // 치명적이지 않으므로 무시하고 계속 진행
        }
    }

    /**
     * S3에서 비동기로 데이터를 가져와 캐시 갱신
     */
    private fun triggerAsyncRefresh(objectKey: String) {
        CompletableFuture.runAsync(
            {
                try {
                    log.debug("[MediaCache] Async refresh started for key: $objectKey")
                    val fresh = mediaStorage.fetchBinaryByKey(objectKey)
                    put(objectKey, fresh)
                    log.info("[MediaCache] Async refresh completed for key: $objectKey")
                } catch (e: Exception) {
                    log.error("[MediaCache] Async refresh failed for key: $objectKey", e)
                    // Graceful degradation: 캐시는 결국 만료되고 다시 조회됨
                }
            },
            executor,
        )
    }
}
