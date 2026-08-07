package com.neki.pose.infra.cache.fake

import com.neki.pose.external.PoseViewCache
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component
import java.util.concurrent.ConcurrentHashMap

@Component
@Profile("test")
class FakePoseViewCacheAdapter : PoseViewCache {

    private val viewers = ConcurrentHashMap<Long, MutableSet<Long>>()

    override fun addViewer(poseId: Long, userId: Long): Boolean {
        val userSet: MutableSet<Long> = viewers.computeIfAbsent(poseId) { ConcurrentHashMap.newKeySet() }
        return userSet.add(userId)
    }

    fun clearAll() {
        viewers.clear()
    }
}
