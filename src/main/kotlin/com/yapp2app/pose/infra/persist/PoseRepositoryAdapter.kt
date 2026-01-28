package com.yapp2app.pose.infra.persist

import com.yapp2app.common.domain.vo.SortOrder
import com.yapp2app.pose.application.port.PoseRepositoryPort
import com.yapp2app.pose.domain.entity.Pose
import com.yapp2app.pose.infra.persist.jpa.JpaPoseRepository
import com.yapp2app.pose.infra.persist.jpa.PosesQueryRepository
import org.springframework.stereotype.Repository

/**
 * fileName       : PoseRepositoryAdapter
 * author         : darren
 * date           : 2026. 1. 27. 17:11
 * description    :
 */
@Repository
class PoseRepositoryAdapter(
    private val jpaRepository: JpaPoseRepository,
    private val queryRepository: PosesQueryRepository,
) : PoseRepositoryPort {

    override fun saveAll(poses: List<Pose>): List<Pose> = jpaRepository.saveAll(poses)

    override fun listPoses(offset: Int, limit: Int, sortOrder: SortOrder): List<Pose> =
        queryRepository.findPoses(offset, limit, sortOrder)
}
