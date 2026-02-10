package com.yapp2app.media.infra.cache.fake

import com.yapp2app.media.application.port.MediaBinaryCachePort
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component
import java.time.Duration
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap

/**
 * fileName       : FakeMediaBinaryAdapter
 * author         : koo
 * date           : 2026. 1. 8. 오후 4:20
 * description    : TTL 추적이 가능한 In-Memory 캐시 어댑터 (테스트 환경 전용)
 */
@Component
@Profile("test")
class FakeMediaBinaryCacheAdapter : MediaBinaryCachePort {

    private val cache = ConcurrentHashMap<String, CacheEntry>()

    data class CacheEntry(val data: ByteArray, val expiresAt: Instant) {
        fun isExpired(): Boolean = Instant.now().isAfter(expiresAt)

        fun remainingTtl(): Duration =
            Duration.between(Instant.now(), expiresAt).takeIf { !it.isNegative } ?: Duration.ZERO
    }

    override fun get(key: String): ByteArray? {
        val entry = cache[key] ?: return null
        return if (entry.isExpired()) {
            cache.remove(key)
            null
        } else {
            entry.data
        }
    }

    override fun put(key: String, value: ByteArray) {
        cache[key] = CacheEntry(
            data = value,
            expiresAt = Instant.now().plus(Duration.ofHours(24)),
        )
    }

    override fun evict(key: String) {
        cache.remove(key)
    }

    // 테스트용 메서드
    fun putWithTtl(key: String, value: ByteArray, ttl: Duration) {
        cache[key] = CacheEntry(
            data = value,
            expiresAt = Instant.now().plus(ttl),
        )
    }

    fun getRemainingTtl(key: String): Duration? = cache[key]?.remainingTtl()

    fun clearAll() {
        cache.clear()
    }

    fun size(): Int = cache.size
}
