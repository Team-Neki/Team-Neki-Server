package com.yapp2app.pose.application.port

interface ScrapPoseRepositoryPort {
    fun add(userId: Long, poseId: Long)

    fun delete(userId: Long, poseId: Long)
}
