package com.yapp2app.pose.api.converter

import com.yapp2app.common.properties.AppProperties
import com.yapp2app.pose.api.dto.GetPoseResponse
import com.yapp2app.pose.api.dto.GetPosesResponse
import com.yapp2app.pose.application.result.GetPoseResult
import com.yapp2app.pose.application.result.GetPosesResult
import org.springframework.stereotype.Component

/**
 * fileName       : PoseResultConverter
 * author         : darren
 * date           : 2026. 1. 28. 11:38
 * description    : Pose api layer response 변경을 위한 converter
 */
@Component
class PoseResultConverter(private val appProperties: AppProperties) {
    companion object {
        private const val IMAGE_URL_PATH = "/file/image/"
    }

    fun toGetPosesResponse(result: GetPosesResult): GetPosesResponse = GetPosesResponse(
        items = result.poses.map {
            GetPosesResponse.PoseInfo(
                poseId = it.poseId,
                headCount = it.headCount,
                imageUrl = toImageUrl(it.storageKey),
                scrap = it.scrap,
                contentType = it.contentType,
                createdAt = it.createdAt,
            )
        },
        hasNext = result.hasNext,
    )

    fun toGetPoseResponse(result: GetPoseResult): GetPoseResponse = GetPoseResponse(
        poseId = result.poseId,
        headCount = result.headCount,
        imageUrl = toImageUrl(result.storageKey),
        scrap = result.scrap,
        contentType = result.contentType,
        createdAt = result.createdAt,
    )

    private fun toImageUrl(storageKey: String): String = "${appProperties.server.url}${IMAGE_URL_PATH}$storageKey"
}
