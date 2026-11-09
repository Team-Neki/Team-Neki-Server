package com.neki.domain.pose.dto

import com.neki.domain.pose.models.HeadCount
import com.neki.domain.pose.models.PoseRelation

/**
 * fileName       : PoseCommand
 * author         : koo
 * date           : 2026. 7. 21.
 * description    : Pose domain command
 */
object PoseCommand {
    data class UploadPoses(val userId: Long?, val uploads: List<Item>) {
        data class Item(
            val mediaId: Long,
            val headCount: HeadCount,
            val memo: String?,
            val relation: PoseRelation? = null,
            val sourceKey: String? = null,
        )
    }

    data class UpdatePoseScrap(val userId: Long, val poseId: Long, val scrap: Boolean)
}
