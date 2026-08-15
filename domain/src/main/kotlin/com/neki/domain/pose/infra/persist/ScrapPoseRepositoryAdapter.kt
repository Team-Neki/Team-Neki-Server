package com.neki.domain.pose.infra.persist

import com.neki.domain.pose.infra.persist.jpa.JpaScrapPoseRepository
import com.neki.domain.pose.models.ScrapPose
import com.neki.domain.pose.repository.ScrapPoseRepository
import org.springframework.stereotype.Repository

/**
 * fileName       : ScrapPoseRepositoryAdapter
 * author         : darren
 * date           : 2026. 1. 28
 * description    :
 */
@Repository
class ScrapPoseRepositoryAdapter(private val jpaRepository: JpaScrapPoseRepository) : ScrapPoseRepository {

    override fun add(scrapPose: ScrapPose) {
        jpaRepository.save(scrapPose)
    }

    override fun delete(scrapPose: ScrapPose) {
        jpaRepository.deleteById(scrapPose.id)
    }

    override fun existsOwnedPoseScrap(scrapPose: ScrapPose): Boolean = jpaRepository.existsById(scrapPose.id)
}
