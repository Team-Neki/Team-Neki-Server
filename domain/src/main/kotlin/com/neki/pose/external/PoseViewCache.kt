package com.neki.pose.external

interface PoseViewCache {

    fun addViewer(poseId: Long, userId: Long): Boolean
}
