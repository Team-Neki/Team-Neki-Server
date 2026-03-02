package com.neki.pose.application.port

interface PoseViewCachePort {

    fun addViewer(poseId: Long, userId: Long): Boolean
}
