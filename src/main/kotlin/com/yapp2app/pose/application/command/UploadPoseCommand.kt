package com.yapp2app.pose.application.command

import com.yapp2app.pose.domain.HeadCount

/**
 * fileName       : PoseCommand
 * author         : darren
 * date           : 2026. 1. 27. 17:23
 * description    :
 */
class UploadPoseCommand(val userId: Long?, val uploads: List<UploadItem>) {
    data class UploadItem(val mediaId: Long, val headCount: HeadCount, val memo: String?)
}
