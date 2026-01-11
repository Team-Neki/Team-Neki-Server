package com.yapp2app.common.redis

import com.fasterxml.jackson.databind.ObjectMapper
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
 * 클린 아키텍처 관점에서 RedisTemplate을 추상화하는 간단한 래퍼
 * - 도메인 로직은 이 서비스에 의존 (인프라 직접 의존 방지)
 * - RedisTemplate의 복잡도를 숨기고 간단한 인터페이스 제공
 * - Jackson2JsonRedisSerializer로 자동 직렬화/역직렬화 (성능 최적화)
 */
@Service
class RedisCacheService(
    private val redisTemplate: RedisTemplate<String, Any>,
    private val objectMapper: ObjectMapper,
) {

    private val log = LoggerFactory.getLogger(javaClass)

    /**
     * 캐시 데이터 조회
     * - Jackson2JsonRedisSerializer가 자동으로 역직렬화
     *
     * @param key 캐시 키 (예: "oidcPublicKeys:kakao")
     * @param clazz 반환 타입
     * @return 캐시된 데이터 또는 null
     */
    fun <T> get(key: String, clazz: Class<T>): T? {
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
    fun set(key: String, value: Any, ttl: Duration) {
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
    fun evict(key: String) {
        redisTemplate.delete(key)
    }

    /**
     * 캐시 존재 여부 확인
     *
     * @param key 캐시 키
     * @return 존재하면 true
     */
    fun exists(key: String): Boolean = redisTemplate.hasKey(key)
}
