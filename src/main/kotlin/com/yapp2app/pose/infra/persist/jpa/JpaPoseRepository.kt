package com.yapp2app.pose.infra.persist.jpa

import com.yapp2app.pose.domain.entity.Pose
import org.springframework.data.jpa.repository.JpaRepository

/**
 * fileName       : JpaPoseRepository
 * author         : darren
 * date           : 2026. 1. 27. 17:10
 * description    : post jpa repository
 */
interface JpaPoseRepository : JpaRepository<Pose, Long> {
    fun existsByUserIdAndId(userId: Long, poseId: Long): Boolean
}
