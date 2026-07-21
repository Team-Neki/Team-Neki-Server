package com.neki.pose.api.converter

import com.neki.common.properties.AppProperties
import com.neki.pose.api.dto.GetPosesResponse
import com.neki.pose.application.dto.PoseResult
import org.springframework.stereotype.Component

/**
 * fileName       : ScrapPoseResultConverter
 * author         : darren
 * date           : 2026. 1. 28. 11:38
 * description    : Pose Scrap api layer response 변경을 위한 converter
 */
@Component
class ScrapPoseResultConverter(private val appProperties: AppProperties) {
    companion object {
        private const val IMAGE_URL_PATH = "/file/image/"
    }

    fun toGetPosesResponse(result: PoseResult.GetPoses): GetPosesResponse = GetPosesResponse(
        items = result.poses.map {
            GetPosesResponse.PoseInfo(
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
