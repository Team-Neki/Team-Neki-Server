package com.yapp2app.pose.application.usecase

import com.yapp2app.common.annotation.UseCase
import com.yapp2app.common.api.dto.ResultCode
import com.yapp2app.common.exception.BusinessException
import com.yapp2app.pose.application.command.GetRandomPoseCommand
import com.yapp2app.pose.application.port.MediaClientPort
import com.yapp2app.pose.application.port.PoseRepositoryPort
import com.yapp2app.pose.application.port.ScrapPoseRepositoryPort
import com.yapp2app.pose.application.result.GetRandomPoseResult
import kotlin.random.Random

@UseCase
class RandomPoseUseCase(
    private val poseRepository: PoseRepositoryPort,
    private val scrapPoseRepository: ScrapPoseRepositoryPort,
    private val mediaClient: MediaClientPort,
) {

    fun execute(command: GetRandomPoseCommand): GetRandomPoseResult {
        val count = poseRepository.countPoses()
        if (count == 0L) {
            throw BusinessException(ResultCode.NOT_FOUND)
        }

        val randomOffset = Random.nextLong(count)
        val pose = poseRepository.findPoseByOffset(randomOffset)
            ?: throw BusinessException(ResultCode.NOT_FOUND)

        val isScrap = scrapPoseRepository.existsOwnedPoseScrap(command.userId, pose.id!!)

        val mediaInfo = mediaClient.getMediaStorageInfo(pose.userId!!, pose.mediaId)

        return GetRandomPoseResult(
            poseId = pose.id!!,
            headCount = pose.headCount,
            storageKey = mediaInfo.storageKey,
            scrap = isScrap,
            contentType = mediaInfo.contentType,
            createdAt = pose.createdAt!!,
        )
    }
}
