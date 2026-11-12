package com.neki.api.pose.api.dto

import com.neki.api.pose.application.dto.PoseResult
import com.neki.core.domain.vo.Pagination
import com.neki.core.properties.AppProperties
import com.neki.domain.pose.dto.PoseCommand
import com.neki.domain.pose.dto.PoseQuery
import com.neki.domain.pose.models.HeadCount
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
                    relation = item.relation,
                    sourceKey = item.sourceKey,
                )
            },
        )

        /**
         * 정렬은 최신순 고정이라 sortOrder 는 받지 않는다 (Pagination 기본값).
         */
        fun toGetPosesQuery(userId: Long, request: PoseRequest.GetPoses): PoseQuery.GetPoses = PoseQuery.GetPoses(
            userId = userId,
            headCount = request.filterGroup.headCountFilter?.headCount,
            pagination = Pagination(page = request.page, size = request.size),
            relation = request.filterGroup.relationFilter?.relation,
        )

        /**
         * deprecated GET /api/poses 용. 관계 필터는 POST /api/poses/filter 의 filterGroup 으로만 받는다.
         */
        fun toGetPosesQuery(userId: Long, page: Int, size: Int, headCount: HeadCount?): PoseQuery.GetPoses =
            PoseQuery.GetPoses(
                userId = userId,
                headCount = headCount,
                pagination = Pagination(page = page, size = size),
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
                    relation = it.relation,
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
            relation = result.relation,
        )

        private fun toImageUrl(storageKey: String): String = "${appProperties.server.url}${IMAGE_URL_PATH}$storageKey"
    }
}
