package com.neki.pose.api.converter

import com.neki.common.domain.vo.SortOrder
import com.neki.pose.api.dto.UpdatePoseScarpRequest
import com.neki.pose.application.dto.PoseCommand
import com.neki.pose.application.dto.PoseQuery
import org.springframework.stereotype.Component

/**
 * fileName       : ScrapPoseCommandConverter
 * author         : darren
 * date           : 2026. 1. 28
 * description    :
 */
@Component
class ScrapPoseCommandConverter {

    fun toUpdatePoseScrapCommand(
        userId: Long,
        poseId: Long,
        request: UpdatePoseScarpRequest,
    ): PoseCommand.UpdatePoseScrap =
        PoseCommand.UpdatePoseScrap(userId = userId, poseId = poseId, scrap = request.scrap!!)

    fun toGetPoseScrapCommand(userId: Long, page: Int, size: Int, sortOrder: SortOrder): PoseQuery.GetScrapPoses =
        PoseQuery.GetScrapPoses(userId = userId, page = page, size = size, headCount = null, sortOrder = sortOrder)
}
