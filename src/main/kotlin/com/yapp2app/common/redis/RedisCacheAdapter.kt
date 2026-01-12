package com.yapp2app.common.redis

import com.fasterxml.jackson.databind.ObjectMapper
import com.yapp2app.common.redis.port.CachePort
import org.slf4j.LoggerFactory
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Service
import java.time.Duration

/**
 * fileName       : RedisCacheService
 * author         : darren
 * date           : 2026. 01. 12.
 * description    : Redis 캐시 직접 제어 서비스
 *
 */
@Service
class RedisCacheAdapter(
    private val redisTemplate: RedisTemplate<String, Any>,
    private val objectMapper: ObjectMapper,
) : CachePort {

    private val log = LoggerFactory.getLogger(javaClass)

    /**
     * 캐시 데이터 조회
     * - Jackson2JsonRedisSerializer가 자동으로 역직렬화
     *
     * @param key 캐시 키 (예: "oidcPublicKeys:kakao")
     * @param clazz 반환 타입
     * @return 캐시된 데이터 또는 null
     */
    override fun <T> get(key: String, clazz: Class<T>): T? {
        val value = redisTemplate.opsForValue().get(key)

        if (value == null) {
            return null
        }

        return try {
            val result = objectMapper.convertValue(value, clazz)
            result
        } catch (e: Exception) {
            log.error("❌ [Cache] Failed to deserialize cache for key: $key", e)
            null
        }
    }

    /**
     * 캐시 데이터 저장
     * - Jackson2JsonRedisSerializer가 자동으로 직렬화
     *
     * @param key 캐시 키
     * @param value 저장할 데이터
     * @param ttl Time To Live (만료 시간)
     */
    override fun set(key: String, value: Any, ttl: Duration) {
        try {
            redisTemplate.opsForValue().set(key, value, ttl)
        } catch (e: Exception) {
            log.error("Failed to serialize cache for key: $key", e)
        }
    }

    /**
     * 캐시 데이터 삭제
     *
     * @param key 캐시 키
     */
    override fun evict(key: String) {
        redisTemplate.delete(key)
    }

    /**
     * 캐시 존재 여부 확인
     *
     * @param key 캐시 키
     * @return 존재하면 true
     */
    override fun exists(key: String): Boolean = redisTemplate.hasKey(key)
}
