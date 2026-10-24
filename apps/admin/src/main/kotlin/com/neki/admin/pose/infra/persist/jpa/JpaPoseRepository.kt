package com.neki.admin.pose.infra.persist.jpa

import com.neki.domain.pose.models.Pose
import org.springframework.data.jpa.repository.JpaRepository

/**
 * fileName       : JpaPoseRepository
 * author         : koo
 * date           : 2026. 8. 16.
 * description    : Pose JPA Repository
 */
interface JpaPoseRepository : JpaRepository<Pose, Long>
