package com.neki.pose.dto

import com.neki.common.domain.vo.Pagination
import com.neki.pose.models.HeadCount

/**
 * fileName       : PoseQuery
 * author         : koo
 * date           : 2026. 7. 21.
 * description    : Pose domain query
 */
object PoseQuery {
    data class GetPoses(val userId: Long, val headCount: HeadCount?, val pagination: Pagination)

    data class GetScrapPoses(val userId: Long, val headCount: HeadCount?, val pagination: Pagination)

    data class GetPose(val userId: Long, val poseId: Long)

    data class GetRandomPose(val userId: Long, val headCount: HeadCount, val excludeIds: List<Long>)
}
