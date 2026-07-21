package com.neki.pose.application.usecase

import com.neki.common.annotation.UseCase
import com.neki.common.transaction.TransactionRunner
import com.neki.pose.application.contract.MediaStorageInfo
import com.neki.pose.application.contract.PoseWithScrap
import com.neki.pose.application.dto.PoseQuery
import com.neki.pose.application.dto.PoseResult
import com.neki.pose.application.port.MediaClientPort
import com.neki.pose.application.port.PoseRepositoryPort
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * fileName       : GetPosesUseCase
 * author         : darren
 * date           : 2026. 1. 28. 11:31
 * description    : pose 목록 조회
 */
@UseCase
class GetPosesUseCase(
    private val poseRepository: PoseRepositoryPort,
    private val mediaClient: MediaClientPort,
    private val transactionRunner: TransactionRunner,
) {
    private val log: Logger = LoggerFactory.getLogger(javaClass)

    fun execute(query: PoseQuery.GetPoses): PoseResult.GetPoses {
        // size + 1개 조회하여 hasNext 판단
        val fetchSize = query.size + 1

        val poses: List<PoseWithScrap> = transactionRunner.readOnly {
            poseRepository.listPosesWithScrap(
                userId = query.userId,
                offset = query.page * query.size,
                limit = fetchSize,
                headCount = query.headCount,
                sortOrder = query.sortOrder,
            )
        }

        if (poses.isEmpty()) {
            return PoseResult.GetPoses(poses = emptyList(), hasNext = false)
        }

        // hasNext 판단: size + 1개 조회했는데 실제로 그만큼 있으면 다음 페이지 존재
        val hasNext = poses.size > query.size

        // 실제 반환할 사진 목록 (size개만)
        val posesToReturn = if (hasNext) poses.dropLast(1) else poses

        // storageKey 조회 (페이징된 결과에 대해서만)
        val mediaStorageInfos: List<MediaStorageInfo> = mediaClient.getMediaStorageInfos(
            posesToReturn.map { it.pose.mediaId },
        )

        val mediaByFileId: Map<Long, MediaStorageInfo> = mediaStorageInfos.associateBy { it.mediaId }

        // 아직 저장되지 않은 이미지가 있다면 일부만 먼저 반환, eventually consistent
        val result: List<PoseResult.GetPoses.PoseInfo> = posesToReturn.mapNotNull { (pose, isScraped) ->
            val media: MediaStorageInfo = mediaByFileId[pose.mediaId]
                ?: run {
                    log.info(
                        "Media not found yet. photoId={}, fileId={}",
                        pose.id,
                        pose.mediaId,
                    )
                    return@mapNotNull null
                }

            PoseResult.GetPoses.PoseInfo(
                poseId = pose.id!!,
                headCount = pose.headCount,
                storageKey = media.storageKey,
                scrap = isScraped,
                contentType = media.contentType,
                width = media.width,
                height = media.height,
                createdAt = pose.createdAt!!,
            )
        }

        return PoseResult.GetPoses(result, hasNext)
    }
}
