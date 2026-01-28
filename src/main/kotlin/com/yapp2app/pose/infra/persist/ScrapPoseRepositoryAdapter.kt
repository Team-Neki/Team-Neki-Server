package com.yapp2app.pose.infra.persist

import com.yapp2app.pose.application.port.ScrapPoseRepositoryPort
import com.yapp2app.pose.domain.entity.ScrapPose
import com.yapp2app.pose.domain.entity.ScrapPoseId
import com.yapp2app.pose.infra.persist.jpa.JpaScrapPoseRepository
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
}
