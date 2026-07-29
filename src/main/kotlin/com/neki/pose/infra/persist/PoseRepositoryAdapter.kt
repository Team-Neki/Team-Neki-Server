package com.neki.pose.infra.persist

import com.neki.common.domain.vo.SortOrder
import com.neki.pose.application.port.PoseRepositoryPort
import com.neki.pose.application.port.dto.PoseContract
import com.neki.pose.domain.HeadCount
import com.neki.pose.domain.entity.Pose
import com.neki.pose.infra.persist.jpa.JpaPoseRepository
import com.neki.pose.infra.persist.jpa.PosesQueryRepository
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

    override fun getOwnedPoseWithScrap(userId: Long, poseId: Long): PoseContract.PoseWithScrap? =
        queryRepository.findOwnedPoseWithScrap(userId, poseId)

    override fun saveAll(poses: List<Pose>): List<Pose> = jpaRepository.saveAll(poses)

    override fun listPosesWithScrap(
        userId: Long,
        offset: Int,
        limit: Int,
        headCount: HeadCount?,
        sortOrder: SortOrder,
    ): List<PoseContract.PoseWithScrap> =
        queryRepository.listPosesWithScrap(userId, offset, limit, headCount, sortOrder)

    override fun listOwnedScrapPoses(userId: Long, offset: Int, limit: Int, sortOrder: SortOrder): List<Pose> =
        queryRepository.findOwnedScrapPoses(userId, offset, limit, sortOrder)

    override fun existsPose(poseId: Long): Boolean = jpaRepository.existsById(poseId)

    override fun countPoses(headCount: HeadCount, excludeIds: List<Long>): Long =
        queryRepository.countPoses(headCount, excludeIds)

    override fun findPoseByOffset(offset: Long, headCount: HeadCount, excludeIds: List<Long>): Pose? =
        queryRepository.findPoseByOffset(offset, headCount, excludeIds)

    override fun incrementViewCount(poseId: Long) {
        queryRepository.incrementViewCount(poseId)
    }
}
