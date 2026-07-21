package com.neki.pose.application.usecase

import com.neki.common.annotation.UseCase
import com.neki.common.code.ResultCode
import com.neki.common.exception.BusinessException
import com.neki.pose.application.contract.MediaStorageInfo
import com.neki.pose.application.dto.PoseQuery
import com.neki.pose.application.dto.PoseResult
import com.neki.pose.application.port.MediaClientPort
import com.neki.pose.application.port.PoseRepositoryPort
import com.neki.pose.application.port.RandomGeneratorPort
import com.neki.pose.application.port.ScrapPoseRepositoryPort
import com.neki.pose.domain.entity.Pose
import com.neki.pose.domain.entity.ScrapPose

@UseCase
class RandomPoseUseCase(
    private val poseRepository: PoseRepositoryPort,
    private val scrapPoseRepository: ScrapPoseRepositoryPort,
    private val mediaClient: MediaClientPort,
    private val randomGenerator: RandomGeneratorPort,
) {

    fun execute(query: PoseQuery.GetRandomPose): PoseResult.GetPose {
        val count: Long = poseRepository.countPoses(query.headCount, query.excludeIds)
        if (count == 0L) {
            throw BusinessException(ResultCode.NO_MORE_RANDOM_POSE)
        }

        val randomOffset: Long = randomGenerator.nextLong(count)

        val pose: Pose = poseRepository.findPoseByOffset(randomOffset, query.headCount, query.excludeIds)
            ?: throw BusinessException(ResultCode.NO_MORE_RANDOM_POSE)

        val isScraped: Boolean = scrapPoseRepository.existsOwnedPoseScrap(ScrapPose(query.userId, pose.id!!))

        val mediaInfo: MediaStorageInfo = mediaClient.getMediaStorageInfo(pose.mediaId)

        return PoseResult.GetPose(
            poseId = pose.id,
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
