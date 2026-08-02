package com.neki.pose.infra.persist.jpa

import com.neki.pose.entity.ScrapPose
import com.neki.pose.entity.ScrapPoseId
import org.springframework.data.jpa.repository.JpaRepository

/**
 * fileName       : JpaScrapPoseRepository
 * author         : darren
 * date           : 2026. 1. 28
 * description    :
 */
interface JpaScrapPoseRepository : JpaRepository<ScrapPose, ScrapPoseId>
