package com.neki.domain.pose.infra.persist.jpa

import com.neki.domain.pose.models.Pose
import org.springframework.data.jpa.repository.JpaRepository

/**
 * fileName       : JpaPoseRepository
 * author         : darren
 * date           : 2026. 1. 27. 17:10
 * description    : post jpa repository
 */
interface JpaPoseRepository : JpaRepository<Pose, Long>
