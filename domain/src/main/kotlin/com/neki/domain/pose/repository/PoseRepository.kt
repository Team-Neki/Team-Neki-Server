package com.neki.domain.pose.repository

import com.neki.core.domain.vo.SortOrder
import com.neki.domain.pose.dto.PoseQuery
import com.neki.domain.pose.models.HeadCount
import com.neki.domain.pose.models.Pose
import com.neki.domain.pose.models.PoseRelation
import com.neki.domain.pose.models.PoseWithScrap

/**
 * fileName       : PoseRepository
 * author         : darren
 * date           : 2026. 1. 27. 17:12
 * description    :
 */
interface PoseRepository {

    fun getOwnedPoseWithScrap(userId: Long, poseId: Long): PoseWithScrap?

    fun findById(poseId: Long): Pose?

    fun existsByMediaIdIn(mediaIds: List<Long>): Boolean

    fun saveAll(poses: List<Pose>): List<Pose>

    /**
     * 최신순(created_at DESC, id DESC) 고정. headCount 와 relation 은 null 이면 거르지 않는다.
     */
    fun listPosesWithScrap(
        userId: Long,
        offset: Int,
        limit: Int,
        headCount: HeadCount?,
        relation: PoseRelation?,
    ): List<PoseWithScrap>

    /**
     * 주어진 source_key 중 이미 포즈로 등록된 것만 돌려준다.
     */
    fun findExistingSourceKeys(sourceKeys: Collection<String>): Set<String>

    fun listOwnedScrapPoses(userId: Long, offset: Int, limit: Int, sortOrder: SortOrder): List<Pose>

    fun findAll(query: PoseQuery.GetAllPoses): List<Pose>

    fun count(query: PoseQuery.GetAllPoses): Long

    fun existsPose(poseId: Long): Boolean

    fun countPoses(headCount: HeadCount, excludeIds: List<Long>): Long

    fun findPoseByOffset(offset: Long, headCount: HeadCount, excludeIds: List<Long>): Pose?
}
