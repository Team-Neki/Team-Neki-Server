package com.neki.pose.application.dto

import com.neki.pose.domain.HeadCount

/**
 * fileName       : PoseCommand
 * author         : koo
 * date           : 2026. 7. 21.
 * description    : Pose domain command
 */
object PoseCommand {
    data class UploadPoses(val userId: Long, val uploads: List<UploadItem>) {
        data class UploadItem(val mediaId: Long, val headCount: HeadCount, val memo: String?)
    }

    data class UpdatePoseScrap(val userId: Long, val poseId: Long, val scrap: Boolean)
}
