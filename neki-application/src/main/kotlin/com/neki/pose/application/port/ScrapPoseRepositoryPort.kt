package com.neki.pose.application.port

import com.neki.pose.entity.ScrapPose

interface ScrapPoseRepositoryPort {
    fun add(scrapPose: ScrapPose)

    fun delete(scrapPose: ScrapPose)

    fun existsOwnedPoseScrap(scrapPose: ScrapPose): Boolean
}
