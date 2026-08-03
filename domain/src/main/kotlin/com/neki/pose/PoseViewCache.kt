package com.neki.pose

interface PoseViewCache {

    fun addViewer(poseId: Long, userId: Long): Boolean
}
