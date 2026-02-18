package com.neki.pose.application.usecase

import com.neki.common.annotation.UseCase
import com.neki.common.api.dto.ResultCode
import com.neki.common.exception.BusinessException
import com.neki.pose.application.command.GetPoseCommand
import com.neki.pose.application.contract.MediaStorageInfo
import com.neki.pose.application.port.MediaClientPort
import com.neki.pose.application.port.PoseRepositoryPort
import com.neki.pose.application.result.GetPoseResult

@UseCase
class GetPoseUseCase(private val poseRepository: PoseRepositoryPort, private val mediaClient: MediaClientPort) {

    fun execute(command: GetPoseCommand): GetPoseResult {
        val (pose, isScraped) = poseRepository.getOwnedPoseWithScrap(command.userId, command.poseId)
            ?: throw BusinessException(ResultCode.NOT_FOUND)

        val mediaInfo: MediaStorageInfo = mediaClient.getMediaStorageInfo(pose.mediaId)

        return GetPoseResult(
            poseId = pose.id!!,
            headCount = pose.headCount,
            storageKey = mediaInfo.storageKey,
            scrap = isScraped,
            contentType = mediaInfo.contentType,
            width = mediaInfo.width,
            height = mediaInfo.height,
            createdAt = pose.createdAt!!,
        )
    }
}
