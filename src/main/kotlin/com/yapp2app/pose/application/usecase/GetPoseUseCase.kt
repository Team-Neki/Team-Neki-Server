package com.yapp2app.pose.application.usecase

import com.yapp2app.common.annotation.UseCase
import com.yapp2app.common.api.dto.ResultCode
import com.yapp2app.common.exception.BusinessException
import com.yapp2app.pose.application.command.GetPoseCommand
import com.yapp2app.pose.application.contract.MediaStorageInfo
import com.yapp2app.pose.application.port.MediaClientPort
import com.yapp2app.pose.application.port.PoseRepositoryPort
import com.yapp2app.pose.application.result.GetPoseResult

@UseCase
class GetPoseUseCase(private val poseRepository: PoseRepositoryPort, private val mediaClient: MediaClientPort) {

    fun execute(command: GetPoseCommand): GetPoseResult {
        val (pose, isScraped) = poseRepository.getOwnedPoseWithScrap(command.userId, command.poseId)
            ?: throw BusinessException(ResultCode.NOT_FOUND)

        val mediaInfo: MediaStorageInfo = mediaClient.getMediaStorageInfo(command.userId, pose.mediaId)

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
