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
 * description    : TTL 추적이 가능한 In-Memory 캐시 어댑터 (prod 외 환경: staging·local·test)
 *
 * staging 도 이 어댑터를 쓰므로 서버 메모리를 무한정 쓰지 않게 한다.
 * - 저장할 때 만료된 항목을 지운다 (다시 조회되지 않는 항목도 남지 않게)
 * - 전체 크기가 maxBytes 를 넘으면 만료가 가장 가까운 항목부터 지운다
 */
@Component
@Profile("!prod")
class FakeMediaBinaryCacheAdapter(private val maxBytes: Long = DEFAULT_MAX_BYTES) : MediaBinaryCache {

    private val cache = ConcurrentHashMap<String, CacheEntry>()
    private var totalBytes: Long = 0

    override fun get(key: String): ByteArray? {
        val entry = cache[key] ?: return null
        return if (entry.isExpired()) {
            evict(key)
            null
        } else {
            entry.data
        }
    }

    @Synchronized
    override fun put(key: String, value: ByteArray, ttl: Duration) {
        if (value.size > maxBytes) return // 상한보다 큰 값은 저장하지 않는다

        removeEntry(key)
        removeExpired()
        while (totalBytes + value.size > maxBytes) {
            val soonest: String = cache.minByOrNull { it.value.expiresAt }?.key ?: break
            removeEntry(soonest)
        }

        cache[key] = CacheEntry(data = value, expiresAt = Instant.now().plus(ttl))
        totalBytes += value.size
    }

    @Synchronized
    override fun evict(key: String) {
        removeEntry(key)
    }

    /** 테스트 확인용: 현재 저장된 바이트 합계 */
    fun storedBytes(): Long = totalBytes

    private fun removeExpired() {
        cache.filterValues { it.isExpired() }.keys.forEach { removeEntry(it) }
    }

    private fun removeEntry(key: String) {
        cache.remove(key)?.let { totalBytes -= it.data.size }
    }

    companion object {
        const val DEFAULT_MAX_BYTES: Long = 100L * 1024 * 1024
    }
}

data class CacheEntry(val data: ByteArray, val expiresAt: Instant) {
    fun isExpired(): Boolean = Instant.now().isAfter(expiresAt)
}
