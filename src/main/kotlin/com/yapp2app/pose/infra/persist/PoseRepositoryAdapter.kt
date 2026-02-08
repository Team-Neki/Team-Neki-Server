package com.yapp2app.pose.infra.persist

import com.yapp2app.common.domain.vo.SortOrder
import com.yapp2app.pose.application.contract.PoseWithScrap
import com.yapp2app.pose.application.port.PoseRepositoryPort
import com.yapp2app.pose.domain.HeadCount
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
}
