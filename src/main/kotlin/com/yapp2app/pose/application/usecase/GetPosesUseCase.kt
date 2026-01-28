package com.yapp2app.pose.application.usecase

import com.yapp2app.common.annotation.UseCase
import com.yapp2app.pose.application.command.GetPosesCommand
import com.yapp2app.pose.application.port.MediaClientPort
import com.yapp2app.pose.application.port.PoseRepositoryPort
import com.yapp2app.pose.application.result.GetPosesResult
import com.yapp2app.pose.domain.entity.Pose
import org.slf4j.LoggerFactory

/**
 * fileName       : GetPosesUseCase
 * author         : darren
 * date           : 2026. 1. 28. 11:31
 * description    : pose 목록 조회
 */
@UseCase
class GetPosesUseCase(private val poseRepository: PoseRepositoryPort, private val mediaClient: MediaClientPort) {
    private val log = LoggerFactory.getLogger(javaClass)

    fun execute(command: GetPosesCommand): GetPosesResult {
        // size + 1개 조회하여 hasNext 판단
        val fetchSize = command.size + 1

        val poses: List<Pose> = poseRepository.listPoses(
            offset = command.page * command.size,
            limit = fetchSize,
            sortOrder = command.sortOrder,
        )

        if (poses.isEmpty()) {
            return GetPosesResult(poses = emptyList(), hasNext = false)
        }

        // hasNext 판단: size + 1개 조회했는데 실제로 그만큼 있으면 다음 페이지 존재
        val hasNext = poses.size > command.size

        // 실제 반환할 사진 목록 (size개만)
        val posesToReturn = if (hasNext) poses.dropLast(1) else poses

        // storageKey 조회 (페이징된 결과에 대해서만)
        val mediaStorageInfos = mediaClient.getMediaStorageInfos(
            posesToReturn.map { it.mediaId },
        )

        val mediaByFileId = mediaStorageInfos.associateBy { it.mediaId }

        // 아직 저장되지 않은 이미지가 있다면 일부만 먼저 반환, eventually consistent
        val result = posesToReturn.mapNotNull { pose ->
            val media = mediaByFileId[pose.mediaId]
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
                storageKey = media.storageKey,
                contentType = media.contentType,
                createdAt = pose.createdAt!!,
            )
        }

        return GetPosesResult(result, hasNext)
    }
}
