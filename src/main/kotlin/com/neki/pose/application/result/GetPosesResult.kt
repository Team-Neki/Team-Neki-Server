package com.neki.pose.application.result

import com.neki.pose.domain.HeadCount
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
        val scrap: Boolean,
        val contentType: String,
        val width: Int? = null,
        val height: Int? = null,
        val createdAt: LocalDateTime,
    )
}

data class GetPoseResult(
    val poseId: Long,
    val headCount: HeadCount,
    val storageKey: String,
    val scrap: Boolean,
    val contentType: String,
    val width: Int? = null,
    val height: Int? = null,
    val createdAt: LocalDateTime,
)
