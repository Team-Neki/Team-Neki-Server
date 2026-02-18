package com.neki.pose.application.usecase

import com.neki.common.annotation.UseCase
import com.neki.common.api.dto.ResultCode
import com.neki.common.exception.BusinessException
import com.neki.pose.application.command.GetRandomPoseCommand
import com.neki.pose.application.port.MediaClientPort
import com.neki.pose.application.port.PoseRepositoryPort
import com.neki.pose.application.port.RandomGeneratorPort
import com.neki.pose.application.port.ScrapPoseRepositoryPort
import com.neki.pose.application.result.GetPoseResult

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
