package com.neki.auth.infra.redis

import com.fasterxml.jackson.databind.ObjectMapper
import com.neki.auth.application.contract.OIDCPublicKeysResponse
import com.neki.auth.application.port.AuthCachePort
import org.slf4j.LoggerFactory
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Component
import java.time.Duration

/**
 * fileName       : RedisCacheService
 * author         : darren
 * date           : 2026. 01. 12.
 * description    : Redis 캐시 직접 제어 서비스
 *
 */
@Component
class AuthRedisCacheAdapter(
    private val redisTemplate: RedisTemplate<String, Any>,
    private val objectMapper: ObjectMapper,
) : AuthCachePort {

    private val log = LoggerFactory.getLogger(javaClass)

    override fun setPublicKeys(key: String, value: OIDCPublicKeysResponse, ttl: Duration) {
        try {
            redisTemplate.opsForValue().set(key, value, ttl)
        } catch (e: Exception) {
            log.error("[AuthCache] Failed to serialize cache for key: $key", e)
        }
    }

    override fun getPublicKeys(key: String): OIDCPublicKeysResponse? {
        val value = redisTemplate.opsForValue().get(key) ?: return null

        return try {
            objectMapper.convertValue(value, OIDCPublicKeysResponse::class.java)
        } catch (e: Exception) {
            log.error("[AuthCache] Failed to deserialize OIDC public keys for key: $key", e)
            null
        }
    }

    override fun clearPublicKeys(key: String) {
        redisTemplate.delete(key)
    }
}
