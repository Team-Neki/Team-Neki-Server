package com.neki.api.pose.api.dto

import com.neki.api.pose.application.dto.PoseResult
import com.neki.core.code.ResultCode
import com.neki.core.domain.vo.Pagination
import com.neki.core.exception.BusinessException
import com.neki.core.properties.AppProperties
import com.neki.domain.pose.dto.PoseCommand
import com.neki.domain.pose.dto.PoseQuery
import com.neki.domain.pose.models.HeadCount
import com.neki.domain.pose.models.PoseImageFile
import com.neki.domain.pose.models.PoseRelation
import org.springframework.stereotype.Component
import org.springframework.web.multipart.MultipartFile
import java.text.Normalizer

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
                )
            },
        )

        /**
         * 정렬은 최신순 고정이라 sortOrder 는 받지 않는다 (Pagination 기본값).
         */
        fun toGetPosesQuery(
            userId: Long,
            page: Int,
            size: Int,
            headCount: HeadCount?,
            relation: PoseRelation?,
        ): PoseQuery.GetPoses = PoseQuery.GetPoses(
            userId = userId,
            headCount = headCount,
            pagination = Pagination(page = page, size = size),
            relation = relation,
        )

        fun toGetPoseQuery(userId: Long, poseId: Long): PoseQuery.GetPose =
            PoseQuery.GetPose(userId = userId, poseId = poseId)

        fun toGetRandomPoseQuery(userId: Long, headCount: HeadCount, excludeIds: String): PoseQuery.GetRandomPose {
            val parsedExcludeIds: List<Long> = excludeIds
                .split(",")
                .mapNotNull { it.trim().toLongOrNull() }
            return PoseQuery.GetRandomPose(userId = userId, headCount = headCount, excludeIds = parsedExcludeIds)
        }

        /**
         * pose.csv 의 media_key 가 source_key 다. media.csv 가 있으면 file_name 으로 이미지를 찾고,
         * 없으면 media_key 에 확장자를 붙여 찾는다. 파일명은 경로를 떼고 NFC 로 맞춘다.
         */
        fun toBulkUploadPosesCommand(
            userId: Long,
            poseCsv: MultipartFile,
            mediaCsv: MultipartFile?,
            images: List<MultipartFile>,
        ): PoseCommand.BulkUploadPoses {
            if (poseCsv.isEmpty) throw BusinessException(ResultCode.INVALID_PARAMETER)

            val poseRows: List<PoseCsvParser.PoseRow> = PoseCsvParser.parsePoseCsv(poseCsv.bytes)
            val fileNames: Map<String, String> =
                mediaCsv?.takeUnless { it.isEmpty }?.let { PoseCsvParser.parseMediaCsv(it.bytes) }.orEmpty()

            return PoseCommand.BulkUploadPoses(
                userId = userId,
                rows = poseRows.map { row ->
                    PoseCommand.BulkUploadPoses.Row(
                        lineNumber = row.lineNumber,
                        sourceKey = row.mediaKey,
                        headCount = row.headCount,
                        tag = row.tag,
                        fileName = fileNames[row.mediaKey]?.let { normalizeFileName(it) },
                    )
                },
                images = images.map { file ->
                    PoseImageFile(
                        fileName = normalizeFileName(file.originalFilename.orEmpty()),
                        readBytes = { file.bytes },
                    )
                },
            )
        }

        private fun normalizeFileName(raw: String): String =
            Normalizer.normalize(raw.substringAfterLast('/').substringAfterLast('\\').trim(), Normalizer.Form.NFC)
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

        fun toBulkUploadResponse(result: PoseResult.BulkUploadPoses): PoseResponse.BulkUploadPoses =
            PoseResponse.BulkUploadPoses(
                summary = PoseResponse.BulkUploadPoses.Summary(
                    totalRows = result.totalRows,
                    created = result.created.size,
                    skipped = result.skipped.size,
                    failed = result.failed.size,
                    unmatchedImages = result.unmatchedImages.size,
                ),
                created = result.created.map {
                    PoseResponse.BulkUploadPoses.Created(
                        sourceKey = it.sourceKey,
                        poseId = it.poseId,
                        mediaId = it.mediaId,
                        headCount = it.headCount,
                        relation = it.relation,
                    )
                },
                skipped = result.skipped.map {
                    PoseResponse.BulkUploadPoses.RowOutcome(it.sourceKey, it.line, it.reason.name)
                },
                failed = result.failed.map {
                    PoseResponse.BulkUploadPoses.RowOutcome(it.sourceKey, it.line, it.reason.name)
                },
                unmatchedImages = result.unmatchedImages,
            )

        private fun toImageUrl(storageKey: String): String = "${appProperties.server.url}${IMAGE_URL_PATH}$storageKey"
    }
}
