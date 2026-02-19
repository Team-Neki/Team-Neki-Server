package com.neki.pose.infra.cache.redis

import com.neki.pose.application.port.PoseViewCachePort
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Primary
import org.springframework.context.annotation.Profile
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Component
import java.time.Duration

@Component
@Primary
@Profile("!test")
class RedisPoseViewCacheAdapter(private val redisTemplate: RedisTemplate<String, Any>) : PoseViewCachePort {

    private val log = LoggerFactory.getLogger(javaClass)

    override fun addViewer(poseId: Long, userId: Long): Boolean {
        val key = PoseViewRedisCacheKey.viewKey(poseId)
        return try {
            val added = redisTemplate.opsForSet().add(key, userId) ?: 0
            if (added > 0) {
                redisTemplate.expire(key, VIEW_TTL)
                true
            } else {
                false
            }
        } catch (e: Exception) {
            log.warn("[PoseViewCache] Failed to check viewer for pose: $poseId, user: $userId", e)
            true // fail-open: Redis 장애 시 신규 조회로 처리
        }
    }

    companion object {
        private val VIEW_TTL = Duration.ofHours(24)
    }
}
