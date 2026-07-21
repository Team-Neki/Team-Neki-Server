package com.neki.pose.application.usecase

import com.neki.common.annotation.UseCase
import com.neki.common.code.ResultCode
import com.neki.common.exception.BusinessException
import com.neki.common.transaction.TransactionRunner
import com.neki.pose.application.dto.PoseQuery
import com.neki.pose.application.dto.PoseResult
import com.neki.pose.application.port.MediaClientPort
import com.neki.pose.application.port.PoseRepositoryPort
import com.neki.pose.application.port.PoseViewCachePort
import com.neki.pose.application.port.dto.MediaContract

@UseCase
class GetPoseUseCase(
    private val poseRepository: PoseRepositoryPort,
    private val mediaClient: MediaClientPort,
    private val poseViewCache: PoseViewCachePort,
    private val transactionRunner: TransactionRunner,
) {

    fun execute(query: PoseQuery.GetPose): PoseResult.GetPose {
        val (pose, isScraped) = poseRepository.getOwnedPoseWithScrap(query.userId, query.poseId)
            ?: throw BusinessException(ResultCode.NOT_FOUND)

        if (poseViewCache.addViewer(query.poseId, query.userId)) {
            transactionRunner.run { poseRepository.incrementViewCount(query.poseId) }
        }

        val mediaInfo: MediaContract.StorageInfo = mediaClient.getMediaStorageInfo(pose.mediaId)

        return PoseResult.GetPose(
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
