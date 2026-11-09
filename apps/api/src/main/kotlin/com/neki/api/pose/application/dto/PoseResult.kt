package com.neki.api.pose.application.dto

import com.neki.domain.pose.models.BulkFailReason
import com.neki.domain.pose.models.BulkSkipReason
import com.neki.domain.pose.models.HeadCount
import com.neki.domain.pose.models.PoseRelation
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
            val relation: PoseRelation? = null,
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
        val relation: PoseRelation? = null,
    )

    /**
     * PM 일괄 업로드 결과. 행 단위로 처리하므로 일부만 성공할 수 있다.
     */
    data class BulkUploadPoses(
        val totalRows: Int,
        val created: List<Created>,
        val skipped: List<Skipped>,
        val failed: List<Failed>,
        val unmatchedImages: List<String>,
    ) {
        data class Created(
            val sourceKey: String,
            val poseId: Long,
            val mediaId: Long,
            val headCount: HeadCount,
            val relation: PoseRelation?,
        )

        data class Skipped(val sourceKey: String, val line: Int, val reason: BulkSkipReason)

        data class Failed(val sourceKey: String, val line: Int, val reason: BulkFailReason)
    }
}
