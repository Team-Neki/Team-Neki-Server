package com.yapp2app.pose.api.converter

import com.yapp2app.pose.api.converter.dto.UploadPoseRequest
import com.yapp2app.pose.application.command.UploadPoseCommand
import org.springframework.stereotype.Component

/**
 * fileName       : PoseCommandConverter
 * author         : darren
 * date           : 2026. 1. 27. 17:52
 * description    :
 */
@Component
class PoseCommandConverter {

    fun toUploadPoseCommand(userId: Long?, request: UploadPoseRequest) = UploadPoseCommand(
        userId = userId,
        uploads = request.uploads.map { item ->
            UploadPoseCommand.UploadItem(
                mediaId = item.mediaId!!,
                headCount = item.headCount,
                memo = item.memo,
            )
        },
    )
}
