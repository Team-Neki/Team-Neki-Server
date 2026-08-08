package com.neki.api.pose.infra.persist.jpa

import com.neki.domain.pose.models.ScrapPose
import com.neki.domain.pose.models.ScrapPoseId
import org.springframework.data.jpa.repository.JpaRepository

/**
 * fileName       : JpaScrapPoseRepository
 * author         : darren
 * date           : 2026. 1. 28
 * description    :
 */
interface JpaScrapPoseRepository : JpaRepository<ScrapPose, ScrapPoseId>
