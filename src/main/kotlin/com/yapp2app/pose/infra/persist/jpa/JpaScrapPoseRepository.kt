package com.yapp2app.pose.infra.persist.jpa

import com.yapp2app.pose.domain.entity.ScrapPose
import com.yapp2app.pose.domain.entity.ScrapPoseId
import org.springframework.data.jpa.repository.JpaRepository

/**
 * fileName       : JpaScrapPoseRepository
 * author         : darren
 * date           : 2026. 1. 28
 * description    :
 */
interface JpaScrapPoseRepository : JpaRepository<ScrapPose, ScrapPoseId>
