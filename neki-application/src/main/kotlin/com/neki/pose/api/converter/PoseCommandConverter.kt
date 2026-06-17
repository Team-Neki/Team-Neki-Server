package com.neki.pose.api.converter

import com.neki.common.domain.vo.SortOrder
import com.neki.pose.HeadCount
import com.neki.pose.api.dto.UploadPoseRequest
import com.neki.pose.application.command.GetPoseCommand
import com.neki.pose.application.command.GetPosesCommand
import com.neki.pose.application.command.GetRandomPoseCommand
import com.neki.pose.application.command.UploadPosesCommand
import org.springframework.stereotype.Component

/**
 * fileName       : PoseCommandConverter
 * author         : darren
 * date           : 2026. 1. 27. 17:52
 * description    :
 */
@Component
class PoseCommandConverter {

    fun toUploadPosesCommand(userId: Long, request: UploadPoseRequest) = UploadPosesCommand(
        userId = userId,
        uploads = request.uploads.map { item ->
            UploadPosesCommand.UploadItem(
                mediaId = item.mediaId!!,
                headCount = item.headCount,
                memo = item.memo,
            )
        },
    )

    fun toGetPosesCommand(
        userId: Long,
        page: Int,
        size: Int,
        headCount: HeadCount?,
        sortOrder: SortOrder,
    ): GetPosesCommand = GetPosesCommand(
        userId = userId,
        page = page,
        size = size,
        headCount = headCount,
        sortOrder = sortOrder,
    )

    fun toGetPoseCommand(userId: Long, poseId: Long): GetPoseCommand = GetPoseCommand(userId = userId, poseId = poseId)

    fun toGetRandomPoseCommand(userId: Long, headCount: HeadCount, excludeIds: String): GetRandomPoseCommand {
        val parsedExcludeIds: List<Long> = excludeIds
            .split(",")
            .mapNotNull { it.trim().toLongOrNull() }
        return GetRandomPoseCommand(userId = userId, headCount = headCount, excludeIds = parsedExcludeIds)
    }
}
