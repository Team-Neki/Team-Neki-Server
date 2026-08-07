package com.neki.pose.infra.persist

import com.neki.common.domain.vo.SortOrder
import com.neki.pose.infra.persist.jpa.JpaPoseRepository
import com.neki.pose.infra.persist.jpa.PosesQueryRepository
import com.neki.pose.models.HeadCount
import com.neki.pose.models.Pose
import com.neki.pose.models.PoseWithScrap
import com.neki.pose.repository.PoseRepository
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
) : PoseRepository {

    override fun getOwnedPoseWithScrap(userId: Long, poseId: Long): PoseWithScrap? =
        queryRepository.findOwnedPoseWithScrap(userId, poseId)

    override fun saveAll(poses: List<Pose>): List<Pose> = jpaRepository.saveAll(poses)

    override fun listPosesWithScrap(
        userId: Long,
        offset: Int,
        limit: Int,
        headCount: HeadCount?,
        sortOrder: SortOrder,
    ): List<PoseWithScrap> = queryRepository.listPosesWithScrap(userId, offset, limit, headCount, sortOrder)

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
