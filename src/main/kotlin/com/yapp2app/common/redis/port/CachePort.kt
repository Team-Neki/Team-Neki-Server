package com.yapp2app.common.redis.port

import java.time.Duration

/**
 * fileName       : CachePort
 * author         : darren
 * date           : 2026. 1. 12. 10:18
 * description    : 캐시사용을 위한 Port 인터페이스
 */
interface CachePort {
    fun <T> get(key: String, clazz: Class<T>): T?
    fun set(key: String, value: Any, ttl: Duration)
    fun evict(key: String)
    fun exists(key: String): Boolean

}