package com.neki.pose.application.usecase

import com.neki.common.annotation.UseCase
import com.neki.common.transaction.TransactionRunner
import com.neki.pose.application.command.GetScrapPosesCommand
import com.neki.pose.application.contract.MediaStorageInfo
import com.neki.pose.application.port.MediaClientPort
import com.neki.pose.application.port.PoseRepositoryPort
import com.neki.pose.application.result.GetPosesResult
import com.neki.pose.domain.entity.Pose
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * fileName       : GetScrapPosesUseCase
 * author         : darren
 * date           : 2026. 1. 28
 * description    :
 */
@UseCase
class GetScrapPosesUseCase(
    private val poseRepository: PoseRepositoryPort,
    private val mediaClient: MediaClientPort,
    private val transactionRunner: TransactionRunner,
) {
    private val log: Logger = LoggerFactory.getLogger(javaClass)

    fun execute(command: GetScrapPosesCommand): GetPosesResult {
        // size + 1개 조회하여 hasNext 판단
        val fetchSize = command.size + 1

        val poses: List<Pose> = transactionRunner.readOnly {
            poseRepository.listOwnedScrapPoses(
                userId = command.userId,
                offset = command.page * command.size,
                limit = fetchSize,
                sortOrder = command.sortOrder,
            )
        }

        if (poses.isEmpty()) {
            return GetPosesResult(emptyList(), hasNext = false)
        }

        // hasNext 판단: size + 1개 조회했는데 실제로 그만큼 있으면 다음 페이지 존재
        val hasNext = poses.size > command.size

        // 실제 반환할 사진 목록 (size개만)
        val posesToReturn = if (hasNext) poses.dropLast(1) else poses

        // storageKey 조회 (페이징된 결과에 대해서만)
        val mediaStorageInfos: List<MediaStorageInfo> = mediaClient.getMediaStorageInfos(
            posesToReturn.map { it.mediaId },
        )

        val mediaByFileId: Map<Long, MediaStorageInfo> = mediaStorageInfos.associateBy { it.mediaId }

        // 아직 저장되지 않은 이미지가 있다면 일부만 먼저 반환, eventually consistent
        val result: List<GetPosesResult.PoseInfo> = posesToReturn.mapNotNull { pose ->
            val media: MediaStorageInfo = mediaByFileId[pose.mediaId]
                ?: run {
                    log.info(
                        "Media not found yet. photoId={}, fileId={}",
                        pose.id,
                        pose.mediaId,
                    )
                    return@mapNotNull null
                }

            GetPosesResult.PoseInfo(
                poseId = pose.id!!,
                headCount = pose.headCount,
                storageKey = media.storageKey,
                scrap = true,
                contentType = media.contentType,
                width = media.width,
                height = media.height,
                createdAt = pose.createdAt!!,
            )
        }

        return GetPosesResult(result, hasNext)
    }
}
