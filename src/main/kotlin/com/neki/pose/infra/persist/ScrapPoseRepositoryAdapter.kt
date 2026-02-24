package com.neki.pose.infra.persist

import com.neki.pose.application.port.ScrapPoseRepositoryPort
import com.neki.pose.domain.entity.ScrapPose
import com.neki.pose.domain.entity.ScrapPoseId
import com.neki.pose.infra.persist.jpa.JpaScrapPoseRepository
import org.springframework.stereotype.Repository

/**
 * fileName       : ScrapPoseRepositoryAdapter
 * author         : darren
 * date           : 2026. 1. 28
 * description    :
 */
@Repository
class ScrapPoseRepositoryAdapter(private val jpaRepository: JpaScrapPoseRepository) : ScrapPoseRepositoryPort {

    override fun add(userId: Long, poseId: Long) {
        val id = ScrapPoseId(userId, poseId)

        if (!jpaRepository.existsById(id)) {
            jpaRepository.save(ScrapPose(id))
        }
    }

    override fun delete(userId: Long, poseId: Long) {
        jpaRepository.deleteById(ScrapPoseId(userId, poseId))
    }

    override fun existsOwnedPoseScrap(userId: Long, poseId: Long): Boolean =
        jpaRepository.existsById(ScrapPoseId(userId, poseId))
}
