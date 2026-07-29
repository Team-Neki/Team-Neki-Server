package com.neki.pose.application.port.dto

import com.neki.pose.domain.entity.Pose

/**
 * fileName       : PoseContract
 * author         : koo
 * date           : 2026. 7. 22.
 * description    : Pose repository port 계약 타입 (조회 프로젝션)
 */
object PoseContract {
    data class PoseWithScrap(val pose: Pose, val isScraped: Boolean)
}
