package com.yapp2app.pose.application.command

import com.yapp2app.common.domain.vo.SortOrder
import com.yapp2app.pose.domain.HeadCount

/**
 * fileName       : PoseCommand
 * author         : darren
 * date           : 2026. 1. 27. 17:23
 * description    :
 */
data class UploadPosesCommand(val userId: Long, val uploads: List<UploadItem>) {
    data class UploadItem(val mediaId: Long, val headCount: HeadCount, val memo: String?)
}

data class GetPosesCommand(
    val userId: Long,
    val page: Int,
    val size: Int,
    val headCount: HeadCount?,
    val sortOrder: SortOrder,
)

data class GetScrapPosesCommand(
    val userId: Long,
    val page: Int,
    val size: Int,
    val headCount: HeadCount?,
    val sortOrder: SortOrder,
)

data class UpdatePoseScrapCommand(val userId: Long, val poseId: Long, val scrap: Boolean)

data class GetPoseCommand(val userId: Long, val poseId: Long)

data class GetRandomPoseCommand(val userId: Long, val headCount: HeadCount)
