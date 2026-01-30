package com.yapp2app.pose.api.converter

import com.yapp2app.common.domain.vo.SortOrder
import com.yapp2app.pose.api.dto.UpdatePoseScarpRequest
import com.yapp2app.pose.application.command.GetScrapPosesCommand
import com.yapp2app.pose.application.command.UpdatePoseScrapCommand
import org.springframework.stereotype.Component

/**
 * fileName       : ScrapPoseCommandConverter
 * author         : darren
 * date           : 2026. 1. 28
 * description    :
 */
@Component
class ScrapPoseCommandConverter {

    fun toUpdatePoseScrapCommand(userId: Long, poseId: Long, request: UpdatePoseScarpRequest): UpdatePoseScrapCommand =
        UpdatePoseScrapCommand(userId = userId, poseId = poseId, scrap = request.scrap!!)

    fun toGetPoseScrapCommand(userId: Long, page: Int, size: Int, sortOrder: SortOrder): GetScrapPosesCommand =
        GetScrapPosesCommand(userId = userId, page = page, size = size, headCount = null, sortOrder = sortOrder)
}
