package com.neki.domain.media.infra.cache.fake

import com.neki.domain.media.external.MediaBinaryCache
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
@Profile("!prod")
class FakeMediaBinaryCacheAdapter : MediaBinaryCache {

    private val cache = ConcurrentHashMap<String, CacheEntry>()

    override fun get(key: String): ByteArray? {
        val entry = cache[key] ?: return null
        return if (entry.isExpired()) {
            cache.remove(key)
            null
        } else {
            entry.data
        }
    }

    override fun put(key: String, value: ByteArray, ttl: Duration) {
        cache[key] = CacheEntry(
            data = value,
            expiresAt = Instant.now().plus(ttl),
        )
    }

    override fun evict(key: String) {
        cache.remove(key)
    }
}

data class CacheEntry(val data: ByteArray, val expiresAt: Instant) {
    fun isExpired(): Boolean = Instant.now().isAfter(expiresAt)
}
