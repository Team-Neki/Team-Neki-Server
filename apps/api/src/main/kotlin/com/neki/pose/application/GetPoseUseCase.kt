package com.neki.pose.application

import com.neki.common.annotation.UseCase
import com.neki.common.transaction.TransactionRunner
import com.neki.pose.application.dto.PoseResult
import com.neki.pose.client.MediaClient
import com.neki.pose.dto.PoseQuery
import com.neki.pose.models.MediaMetadata
import com.neki.pose.service.PoseService

@UseCase
class GetPoseUseCase(
    private val poseService: PoseService,
    private val mediaClient: MediaClient,
    private val transactionRunner: TransactionRunner,
) {

    fun execute(query: PoseQuery.GetPose): PoseResult.GetPose {
        val (pose, isScraped) = poseService.getOwnedPoseWithScrap(query)

        if (poseService.isFirstViewOf(query)) {
            transactionRunner.run { poseService.incrementViewCount(query) }
        }

        val mediaInfo: MediaMetadata = mediaClient.getMediaMetadata(pose.mediaId)

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
