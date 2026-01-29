package com.yapp2app.pose.api.converter

import com.yapp2app.common.domain.vo.SortOrder
import com.yapp2app.pose.api.dto.UpdatePoseScarpRequest
import com.yapp2app.pose.application.command.GetPosesCommand
import com.yapp2app.pose.application.command.UpdatePoseScrapCommand
import com.yapp2app.pose.domain.HeadCount
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

    fun toGetPoseScrapCommand(page: Int, size: Int, sortOrder: SortOrder): GetPosesCommand =
        GetPosesCommand(page = page, size = size, headCount = null, sortOrder = sortOrder)
}
