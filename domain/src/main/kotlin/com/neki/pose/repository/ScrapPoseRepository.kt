package com.neki.pose.repository

import com.neki.pose.models.ScrapPose

interface ScrapPoseRepository {
    fun add(scrapPose: ScrapPose)

    fun delete(scrapPose: ScrapPose)

    fun existsOwnedPoseScrap(scrapPose: ScrapPose): Boolean
}
