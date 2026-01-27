package com.yapp2app.pose.infra.persist

import com.yapp2app.pose.application.port.PoseRepositoryPort
import com.yapp2app.pose.domain.entity.Pose
import com.yapp2app.pose.infra.persist.jpa.JpaPoseRepository
import org.springframework.stereotype.Repository

/**
 * fileName       : PoseRepositoryAdapter
 * author         : darren
 * date           : 2026. 1. 27. 17:11
 * description    :
 */
@Repository
class PoseRepositoryAdapter(private val jpaRepository: JpaPoseRepository) : PoseRepositoryPort {

    override fun saveAll(poses: List<Pose>): List<Pose> = jpaRepository.saveAll(poses)
}
