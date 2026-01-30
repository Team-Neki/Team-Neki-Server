package com.yapp2app.pose.api.converter

import com.yapp2app.common.properties.AppProperties
import com.yapp2app.pose.api.dto.GetPosesResponse
import com.yapp2app.pose.application.result.GetPosesResult
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

    fun toGetPosesResponse(result: GetPosesResult): GetPosesResponse = GetPosesResponse(
        items = result.poses.map {
            GetPosesResponse.PoseInfo(
                poseId = it.poseId,
                headCount = it.headCount,
                imageUrl = toImageUrl(it.storageKey),
                contentType = it.contentType,
                createdAt = it.createdAt,
            )
        },
        hasNext = result.hasNext,
    )

    private fun toImageUrl(storageKey: String): String = "${appProperties.server.url}${IMAGE_URL_PATH}$storageKey"
}
