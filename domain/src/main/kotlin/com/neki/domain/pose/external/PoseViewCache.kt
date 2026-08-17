package com.neki.domain.pose.external

interface PoseViewCache {

    fun addViewer(poseId: Long, userId: Long): Boolean
}
