package com.neki.pose.infra.persist

import com.neki.pose.application.port.ScrapPoseRepositoryPort
import com.neki.pose.entity.ScrapPose
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

    override fun add(scrapPose: ScrapPose) {
        jpaRepository.save(scrapPose)
    }

    override fun delete(scrapPose: ScrapPose) {
        jpaRepository.deleteById(scrapPose.id)
    }

    override fun existsOwnedPoseScrap(scrapPose: ScrapPose): Boolean = jpaRepository.existsById(scrapPose.id)
}
