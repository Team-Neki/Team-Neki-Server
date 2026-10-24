package com.neki.domain.pose.repository

import com.neki.core.domain.vo.SortOrder
import com.neki.domain.pose.dto.PoseQuery
import com.neki.domain.pose.models.HeadCount
import com.neki.domain.pose.models.Pose
import com.neki.domain.pose.models.PoseWithScrap

/**
 * fileName       : PoseRepository
 * author         : darren
 * date           : 2026. 1. 27. 17:12
 * description    :
 */
interface PoseRepository {

    fun getOwnedPoseWithScrap(userId: Long, poseId: Long): PoseWithScrap?

    fun findById(poseId: Long): Pose

    fun saveAll(poses: List<Pose>): List<Pose>

    fun listPosesWithScrap(
        userId: Long,
        offset: Int,
        limit: Int,
        headCount: HeadCount?,
        sortOrder: SortOrder,
    ): List<PoseWithScrap>

    fun listOwnedScrapPoses(userId: Long, offset: Int, limit: Int, sortOrder: SortOrder): List<Pose>

    fun findAll(query: PoseQuery.GetAllPoses): List<Pose>

    fun count(query: PoseQuery.GetAllPoses): Long

    fun existsPose(poseId: Long): Boolean

    fun countPoses(headCount: HeadCount, excludeIds: List<Long>): Long

    fun findPoseByOffset(offset: Long, headCount: HeadCount, excludeIds: List<Long>): Pose?

    fun incrementViewCount(poseId: Long)
}
