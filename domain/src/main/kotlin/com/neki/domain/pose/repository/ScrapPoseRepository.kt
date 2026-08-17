package com.neki.domain.pose.repository

import com.neki.domain.pose.models.ScrapPose

interface ScrapPoseRepository {
    fun add(scrapPose: ScrapPose)

    fun delete(scrapPose: ScrapPose)

    fun existsOwnedPoseScrap(scrapPose: ScrapPose): Boolean
}
