package com.yapp2app.pose.api.converter

import com.yapp2app.common.domain.vo.SortOrder
import com.yapp2app.pose.api.dto.UploadPoseRequest
import com.yapp2app.pose.application.command.GetPosesCommand
import com.yapp2app.pose.application.command.UploadPosesCommand
import com.yapp2app.pose.domain.HeadCount
import org.springframework.stereotype.Component

/**
 * fileName       : PoseCommandConverter
 * author         : darren
 * date           : 2026. 1. 27. 17:52
 * description    :
 */
@Component
class PoseCommandConverter {

    fun toUploadPosesCommand(userId: Long?, request: UploadPoseRequest) = UploadPosesCommand(
        userId = userId,
        uploads = request.uploads.map { item ->
            UploadPosesCommand.UploadItem(
                mediaId = item.mediaId!!,
                headCount = item.headCount,
                memo = item.memo,
            )
        },
    )

    fun toGetPosesCommand(page: Int, size: Int, headCount: HeadCount?, sortOrder: SortOrder): GetPosesCommand =
        GetPosesCommand(page = page, size = size, headCount = headCount, sortOrder = sortOrder)
}
