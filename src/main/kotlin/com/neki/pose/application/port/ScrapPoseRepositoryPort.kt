package com.neki.pose.application.port

interface ScrapPoseRepositoryPort {
    fun add(userId: Long, poseId: Long)

    fun delete(userId: Long, poseId: Long)

    fun existsOwnedPoseScrap(userId: Long, poseId: Long): Boolean
}
