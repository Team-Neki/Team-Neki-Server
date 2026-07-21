package com.neki.pose.api.dto

import com.neki.common.domain.vo.SortOrder
import com.neki.common.properties.AppProperties
import com.neki.pose.application.dto.PoseCommand
import com.neki.pose.application.dto.PoseQuery
import com.neki.pose.application.dto.PoseResult
import org.springframework.stereotype.Component

/**
 * fileName       : ScrapPoseConverter
 * author         : koo
 * date           : 2026. 7. 21.
 * description    : ScrapPose api layer converter
 */
object ScrapPoseConverter {
    @Component
    class RequestConverter {
        fun toUpdatePoseScrapCommand(
            userId: Long,
            poseId: Long,
            request: PoseRequest.UpdatePoseScarp,
        ): PoseCommand.UpdatePoseScrap =
            PoseCommand.UpdatePoseScrap(userId = userId, poseId = poseId, scrap = request.scrap!!)

        fun toGetPoseScrapCommand(userId: Long, page: Int, size: Int, sortOrder: SortOrder): PoseQuery.GetScrapPoses =
            PoseQuery.GetScrapPoses(userId = userId, page = page, size = size, headCount = null, sortOrder = sortOrder)
    }

    @Component
    class ResponseConverter(private val appProperties: AppProperties) {
        companion object {
            private const val IMAGE_URL_PATH = "/file/image/"
        }

        fun toGetPosesResponse(result: PoseResult.GetPoses): PoseResponse.GetPoses = PoseResponse.GetPoses(
            items = result.poses.map {
                PoseResponse.GetPoses.PoseInfo(
                    poseId = it.poseId,
                    headCount = it.headCount,
                    imageUrl = toImageUrl(it.storageKey),
                    scrap = it.scrap,
                    contentType = it.contentType,
                    width = it.width,
                    height = it.height,
                    createdAt = it.createdAt,
                )
            },
            hasNext = result.hasNext,
        )

        private fun toImageUrl(storageKey: String): String = "${appProperties.server.url}${IMAGE_URL_PATH}$storageKey"
    }
}
