package com.yapp2app.pose.application.port

import com.yapp2app.common.domain.vo.SortOrder
import com.yapp2app.pose.application.contract.PoseWithScrap
import com.yapp2app.pose.domain.HeadCount
import com.yapp2app.pose.domain.entity.Pose

/**
 * fileName       : PoseRepositoryPort
 * author         : darren
 * date           : 2026. 1. 27. 17:12
 * description    :
 */
interface PoseRepositoryPort {

    fun getOwnedPoseWithScrap(userId: Long, poseId: Long): PoseWithScrap?

    fun saveAll(poses: List<Pose>): List<Pose>

    fun listPoses(offset: Int, limit: Int, headCount: HeadCount?, sortOrder: SortOrder): List<Pose>

    fun existsOwnedPose(userId: Long, poseId: Long): Boolean

    fun countPoses(headCount: HeadCount): Long

    fun findPoseByOffset(offset: Long, headCount: HeadCount): Pose?
}
