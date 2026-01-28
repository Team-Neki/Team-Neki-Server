package com.yapp2app.pose.application.result

import com.yapp2app.pose.domain.HeadCount
import java.time.LocalDateTime

/**
 * fileName       : GetPosesResult
 * author         : darren
 * date           : 2026. 1. 28. 11:33
 * description    :
 */
data class GetPosesResult(val poses: List<PoseInfo>, val hasNext: Boolean) {
    data class PoseInfo(
        val poseId: Long,
        val headCount: HeadCount,
        val storageKey: String,
        val contentType: String,
        val createdAt: LocalDateTime,
    )
}
