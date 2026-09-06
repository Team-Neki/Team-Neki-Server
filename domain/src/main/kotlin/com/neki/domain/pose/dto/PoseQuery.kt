package com.neki.domain.pose.dto

import com.neki.core.domain.vo.Pagination
import com.neki.domain.pose.models.HeadCount

/**
 * fileName       : PoseQuery
 * author         : koo
 * date           : 2026. 7. 21.
 * description    : Pose domain query
 */
object PoseQuery {
    data class GetPoses(val userId: Long, val headCount: HeadCount?, val pagination: Pagination)

    /**
     * 사용자 컨텍스트 없이 전체 포즈를 조회한다. headCount 는 null 이면 거르지 않는다.
     */
    data class GetAllPoses(val headCount: HeadCount?, val pagination: Pagination)

    data class GetScrapPoses(val userId: Long, val headCount: HeadCount?, val pagination: Pagination)

    data class GetPose(val userId: Long, val poseId: Long)

    data class GetRandomPose(val userId: Long, val headCount: HeadCount, val excludeIds: List<Long>)
}
