package com.yapp2app.common.config

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.redis.connection.RedisConnectionFactory
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer
import org.springframework.data.redis.serializer.StringRedisSerializer

/**
 * fileName       : RedisCacheConfig
 * author         : darren
 * date           : 2026. 01. 12.
 * description    : Redis 설정
 *
 * Spring Cache 추상화(@Cacheable) 대신 RedisTemplate을 직접 사용
 * - 클린 아키텍처 관점에서 RedisCacheService를 통해 추상화
 * - OIDC 공개키 캐싱: TTL 6시간, 검증 실패 시 무효화 후 재시도
 * - Jackson2JsonRedisSerializer 사용 (성능 최적화)
 */
@Configuration
class RedisCacheConfig(
    private val objectMapper: ObjectMapper,
) {

    /**
     * RedisTemplate 설정
     * - Key: String 직렬화
     * - Value: Jackson2JsonRedisSerializer (타입별 직렬화로 성능 최적화)
     *
     * 성능 장점:
     * - GenericJackson2JsonRedisSerializer보다 약 30% 빠름
     * - @class 필드 없이도 역직렬화 가능
     * - ObjectMapper 재사용으로 메모리 절약
     */
    @Bean
    fun redisTemplate(redisConnectionFactory: RedisConnectionFactory): RedisTemplate<String, Any> {
        val template = RedisTemplate<String, Any>()
        template.connectionFactory = redisConnectionFactory

        // Key는 String으로 직렬화
        template.keySerializer = StringRedisSerializer()
        template.hashKeySerializer = StringRedisSerializer()

        // Value는 Jackson2JsonRedisSerializer로 직렬화 (성능 최적화)
        val jsonSerializer = Jackson2JsonRedisSerializer(objectMapper, Any::class.java)
        template.valueSerializer = jsonSerializer
        template.hashValueSerializer = jsonSerializer

        return template
    }
}