package com.neki.pose.application.port

import com.neki.common.domain.vo.SortOrder
import com.neki.pose.application.port.dto.PoseContract
import com.neki.pose.domain.HeadCount
import com.neki.pose.domain.entity.Pose

/**
 * fileName       : PoseRepositoryPort
 * author         : darren
 * date           : 2026. 1. 27. 17:12
 * description    :
 */
interface PoseRepositoryPort {

    fun getOwnedPoseWithScrap(userId: Long, poseId: Long): PoseContract.PoseWithScrap?

    fun saveAll(poses: List<Pose>): List<Pose>

    fun listPosesWithScrap(
        userId: Long,
        offset: Int,
        limit: Int,
        headCount: HeadCount?,
        sortOrder: SortOrder,
    ): List<PoseContract.PoseWithScrap>

    fun listOwnedScrapPoses(userId: Long, offset: Int, limit: Int, sortOrder: SortOrder): List<Pose>

    fun existsPose(poseId: Long): Boolean

    fun countPoses(headCount: HeadCount, excludeIds: List<Long>): Long

    fun findPoseByOffset(offset: Long, headCount: HeadCount, excludeIds: List<Long>): Pose?

    fun incrementViewCount(poseId: Long)
}
