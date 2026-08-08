package com.neki.domain.media.external

import java.time.Duration

interface MediaBinaryCache {

    fun get(key: String): ByteArray?

    fun put(key: String, value: ByteArray, ttl: Duration)

    fun evict(key: String)
}
