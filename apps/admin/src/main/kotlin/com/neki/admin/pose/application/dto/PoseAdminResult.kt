package com.neki.admin.pose.application.dto

import com.neki.domain.pose.models.HeadCount
import java.time.LocalDateTime

object PoseAdminResult {
    data class GetPoses(
        val headCount: HeadCount?,
        val totalCount: Long,
        val totalPages: Int,
        val poses: List<PoseInfo>,
    ) {
        data class PoseInfo(
            val id: Long,
            val mediaId: Long,
            val headCount: HeadCount,
            val memo: String?,
            val updatedAt: LocalDateTime,
        )
    }
}
