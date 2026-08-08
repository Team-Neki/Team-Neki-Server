package com.neki.api.pose.infra.cache.redis

import com.neki.domain.pose.external.PoseViewCache
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Primary
import org.springframework.context.annotation.Profile
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Component
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

@Component
@Primary
@Profile("!test")
class RedisPoseViewCacheAdapter(private val redisTemplate: RedisTemplate<String, Any>) : PoseViewCache {

    private val log: Logger = LoggerFactory.getLogger(javaClass)

    override fun addViewer(poseId: Long, userId: Long): Boolean {
        val key: String = PoseViewRedisCacheKey.viewKey(poseId)
        return try {
            val added: Long = redisTemplate.opsForSet().add(key, userId) ?: 0
            if (added > 0) {
                redisTemplate.expireAt(key, getMidnight())
                true
            } else {
                false
            }
        } catch (e: Exception) {
            log.warn("[PoseViewCache] Failed to check viewer for pose: $poseId, user: $userId", e)
            true // fail-open: Redis 장애 시 신규 조회로 처리
        }
    }

    private fun getMidnight(): Instant {
        val tomorrow = LocalDate.now(KOREA_ZONE).plusDays(1)
        return tomorrow.atStartOfDay(KOREA_ZONE).toInstant()
    }

    companion object {
        private val KOREA_ZONE = ZoneId.of("Asia/Seoul")
    }
}
