package com.neki.pose.api.converter

import com.neki.common.domain.vo.SortOrder
import com.neki.pose.api.dto.UploadPoseRequest
import com.neki.pose.application.dto.PoseCommand
import com.neki.pose.application.dto.PoseQuery
import com.neki.pose.domain.HeadCount
import org.springframework.stereotype.Component

/**
 * fileName       : PoseCommandConverter
 * author         : darren
 * date           : 2026. 1. 27. 17:52
 * description    :
 */
@Component
class PoseCommandConverter {

    fun toUploadPosesCommand(userId: Long, request: UploadPoseRequest) = PoseCommand.UploadPoses(
        userId = userId,
        uploads = request.uploads.map { item ->
            PoseCommand.UploadPoses.UploadItem(
                mediaId = item.mediaId!!,
                headCount = item.headCount,
                memo = item.memo,
            )
        },
    )

    fun toGetPosesQuery(
        userId: Long,
        page: Int,
        size: Int,
        headCount: HeadCount?,
        sortOrder: SortOrder,
    ): PoseQuery.GetPoses = PoseQuery.GetPoses(
        userId = userId,
        page = page,
        size = size,
        headCount = headCount,
        sortOrder = sortOrder,
    )

    fun toGetPoseQuery(userId: Long, poseId: Long): PoseQuery.GetPose =
        PoseQuery.GetPose(userId = userId, poseId = poseId)

    fun toGetRandomPoseQuery(userId: Long, headCount: HeadCount, excludeIds: String): PoseQuery.GetRandomPose {
        val parsedExcludeIds: List<Long> = excludeIds
            .split(",")
            .mapNotNull { it.trim().toLongOrNull() }
        return PoseQuery.GetRandomPose(userId = userId, headCount = headCount, excludeIds = parsedExcludeIds)
    }
}
