package com.neki.pose

import com.neki.pose.models.ScrapPose

interface ScrapPoseRepository {
    fun add(scrapPose: ScrapPose)

    fun delete(scrapPose: ScrapPose)

    fun existsOwnedPoseScrap(scrapPose: ScrapPose): Boolean
}
