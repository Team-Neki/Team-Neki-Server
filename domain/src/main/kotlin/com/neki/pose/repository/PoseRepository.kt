package com.neki.pose.repository

import com.neki.common.domain.vo.SortOrder
import com.neki.pose.models.HeadCount
import com.neki.pose.models.Pose
import com.neki.pose.models.PoseWithScrap

/**
 * fileName       : PoseRepository
 * author         : darren
 * date           : 2026. 1. 27. 17:12
 * description    :
 */
interface PoseRepository {

    fun getOwnedPoseWithScrap(userId: Long, poseId: Long): PoseWithScrap?

    fun saveAll(poses: List<Pose>): List<Pose>

    fun listPosesWithScrap(
        userId: Long,
        offset: Int,
        limit: Int,
        headCount: HeadCount?,
        sortOrder: SortOrder,
    ): List<PoseWithScrap>

    fun listOwnedScrapPoses(userId: Long, offset: Int, limit: Int, sortOrder: SortOrder): List<Pose>

    fun existsPose(poseId: Long): Boolean

    fun countPoses(headCount: HeadCount, excludeIds: List<Long>): Long

    fun findPoseByOffset(offset: Long, headCount: HeadCount, excludeIds: List<Long>): Pose?

    fun incrementViewCount(poseId: Long)
}
