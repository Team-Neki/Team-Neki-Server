package com.neki.pose.api.dto

import com.neki.common.domain.vo.Pagination
import com.neki.common.domain.vo.SortOrder
import com.neki.common.properties.AppProperties
import com.neki.pose.application.dto.PoseResult
import com.neki.pose.dto.PoseCommand
import com.neki.pose.dto.PoseQuery
import com.neki.pose.models.HeadCount
import org.springframework.stereotype.Component

/**
 * fileName       : PoseConverter
 * author         : koo
 * date           : 2026. 7. 21.
 * description    : Pose api layer converter
 */
object PoseConverter {
    @Component
    class RequestConverter {
        fun toUploadPosesCommand(userId: Long, request: PoseRequest.UploadPose) = PoseCommand.UploadPoses(
            userId = userId,
            uploads = request.uploads.map { item ->
                PoseCommand.UploadPoses.Item(
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
            headCount = headCount,
            pagination = Pagination(page = page, size = size, sortOrder = sortOrder),
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

    @Component
    class ResponseConverter(private val appProperties: AppProperties) {
        companion object {
            private const val IMAGE_URL_PATH = "/file/image/"
        }

        fun toGetPosesResponse(result: PoseResult.GetPoses): PoseResponse.GetPoses = PoseResponse.GetPoses(
            items = result.poses.map {
                PoseResponse.GetPoses.Item(
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

        fun toGetPoseResponse(result: PoseResult.GetPose): PoseResponse.GetPose = PoseResponse.GetPose(
            poseId = result.poseId,
            headCount = result.headCount,
            imageUrl = toImageUrl(result.storageKey),
            scrap = result.scrap,
            contentType = result.contentType,
            width = result.width,
            height = result.height,
            createdAt = result.createdAt,
        )

        private fun toImageUrl(storageKey: String): String = "${appProperties.server.url}${IMAGE_URL_PATH}$storageKey"
    }
}
