package com.neki.pose.infra.cache.redis

internal object PoseViewRedisCacheKey {
    private const val VIEW_PREFIX = "pose:view:"

    fun viewKey(poseId: Long): String = "$VIEW_PREFIX$poseId"
}
