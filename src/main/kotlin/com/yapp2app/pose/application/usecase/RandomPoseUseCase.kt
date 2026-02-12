package com.yapp2app.pose.application.usecase

import com.yapp2app.common.annotation.UseCase
import com.yapp2app.common.api.dto.ResultCode
import com.yapp2app.common.exception.BusinessException
import com.yapp2app.pose.application.command.GetRandomPoseCommand
import com.yapp2app.pose.application.port.MediaClientPort
import com.yapp2app.pose.application.port.PoseRepositoryPort
import com.yapp2app.pose.application.port.RandomGeneratorPort
import com.yapp2app.pose.application.port.ScrapPoseRepositoryPort
import com.yapp2app.pose.application.result.GetPoseResult

@UseCase
class RandomPoseUseCase(
    private val poseRepository: PoseRepositoryPort,
    private val scrapPoseRepository: ScrapPoseRepositoryPort,
    private val mediaClient: MediaClientPort,
    private val randomGenerator: RandomGeneratorPort,
) {

    fun execute(command: GetRandomPoseCommand): GetPoseResult {
        val count = poseRepository.countPoses(command.headCount, command.excludeIds)
        if (count == 0L) {
            throw BusinessException(ResultCode.NO_MORE_RANDOM_POSE)
        }

        val randomOffset = randomGenerator.nextLong(count)

        val pose = poseRepository.findPoseByOffset(randomOffset, command.headCount, command.excludeIds)
            ?: throw BusinessException(ResultCode.NO_MORE_RANDOM_POSE)

        val isScraped = scrapPoseRepository.existsOwnedPoseScrap(command.userId, pose.id!!)

        val mediaInfo = mediaClient.getMediaStorageInfo(pose.mediaId)

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
