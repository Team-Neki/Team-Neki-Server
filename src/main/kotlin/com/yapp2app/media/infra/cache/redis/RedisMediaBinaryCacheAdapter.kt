package com.yapp2app.media.infra.cache.redis

import com.yapp2app.media.application.port.MediaBinaryCachePort
import com.yapp2app.media.domain.MediaType
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Primary
import org.springframework.context.annotation.Profile
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Component
import java.time.Duration
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
 * - MediaType별 TTL로 메모리 효율성과 캐시 효율성 균형
 * - TTL 기반 lazy refresh로 인기 콘텐츠의 캐시 만료 방지
 */
@Component
@Primary
@Profile("!test")
class RedisMediaBinaryCacheAdapter(private val binaryRedisTemplate: RedisTemplate<String, ByteArray>) :
    MediaBinaryCachePort {

    private val log = LoggerFactory.getLogger(javaClass)

    override fun get(key: String): ByteArray? {
        val cacheKey = MediaRedisCacheKey.binaryKey(key)
        return try {
            val cached = binaryRedisTemplate.opsForValue().get(cacheKey)
            if (cached != null) {
                log.debug("[MediaCache] Cache hit for key: $key")

                // TTL 확인 후 갱신 필요 시 refresh 트리거
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

    override fun put(key: String, value: ByteArray, ttl: Duration) {
        val cacheKey = MediaRedisCacheKey.binaryKey(key)
        try {
            binaryRedisTemplate.opsForValue().set(cacheKey, value, ttl)
            log.debug("[MediaCache] Cache put successful for key: $key (TTL: $ttl)")
        } catch (e: Exception) {
            log.error("[MediaCache] Cache put failed for key: $key", e)
            // 예외를 던지지 않음 - 캐싱 실패해도 서비스는 계속 동작
        }
    }

    override fun evict(key: String) {
        val cacheKey = MediaRedisCacheKey.binaryKey(key)
        try {
            val deleted: Boolean? = binaryRedisTemplate.delete(cacheKey)
            if (deleted == true) {
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
     * TTL을 확인하고 임계값 미만이면 TTL을 연장하여 캐시 만료 방지
     * - 이미 캐싱된 데이터를 재활용하므로 S3 API 호출 불필요
     * - refresh threshold는 해당 MediaType의 cacheTtl / 12로 동적 계산
     */
    private fun checkAndRefreshIfNeeded(objectKey: String, cacheKey: String) {
        try {
            val mediaType = MediaType.fromObjectKey(objectKey) ?: return
            val cacheTtl = mediaType.cacheTtl ?: return
            val refreshThreshold = cacheTtl.dividedBy(12).coerceAtLeast(Duration.ofMinutes(10))

            val ttlSeconds: Long? = binaryRedisTemplate.getExpire(cacheKey, TimeUnit.SECONDS)

            if (ttlSeconds != null && ttlSeconds > 0 && ttlSeconds < refreshThreshold.seconds) {
                binaryRedisTemplate.expire(cacheKey, cacheTtl)
                log.debug(
                    "[MediaCache] TTL extended for key: $objectKey ($ttlSeconds seconds remaining -> ${cacheTtl.seconds} seconds)",
                )
            }
        } catch (e: Exception) {
            log.warn("[MediaCache] TTL check failed for key: $objectKey", e)
        }
    }
}
