package com.neki.media.application.port

import java.time.Duration

/**
 * fileName       : MediaBinaryCachePort
 * author         : koo
 * date           : 2026. 1. 8. 오후 4:19
 * description    : MediaBinary를 가져오기 위한 cache port
 */
interface MediaBinaryCachePort {

    fun get(key: String): ByteArray?

    fun put(key: String, value: ByteArray, ttl: Duration)

    fun evict(key: String)
}
