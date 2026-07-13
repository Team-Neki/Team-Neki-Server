package com.neki.pose.application.port

import com.neki.pose.domain.entity.ScrapPose

interface ScrapPoseRepositoryPort {
    fun add(scrapPose: ScrapPose)

    fun delete(scrapPose: ScrapPose)

    fun existsOwnedPoseScrap(userId: Long, poseId: Long): Boolean
}
