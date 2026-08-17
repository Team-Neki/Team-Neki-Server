package com.neki.api.pose.application.dto

import com.neki.domain.pose.models.HeadCount
import java.time.LocalDateTime

/**
 * fileName       : PoseResult
 * author         : koo
 * date           : 2026. 7. 21.
 * description    : Pose domain result
 */
object PoseResult {
    data class GetPoses(val poses: List<Item>, val hasNext: Boolean) {
        data class Item(
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

    data class GetPose(
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
