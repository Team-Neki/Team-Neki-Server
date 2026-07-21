package com.neki.pose.application.dto

import com.neki.common.domain.vo.SortOrder
import com.neki.pose.domain.HeadCount

/**
 * fileName       : PoseQuery
 * author         : koo
 * date           : 2026. 7. 21.
 * description    : Pose domain query
 */
object PoseQuery {
    data class GetPoses(
        val userId: Long,
        val page: Int,
        val size: Int,
        val headCount: HeadCount?,
        val sortOrder: SortOrder,
    )

    data class GetScrapPoses(
        val userId: Long,
        val page: Int,
        val size: Int,
        val headCount: HeadCount?,
        val sortOrder: SortOrder,
    )

    data class GetPose(val userId: Long, val poseId: Long)

    data class GetRandomPose(val userId: Long, val headCount: HeadCount, val excludeIds: List<Long>)
}
