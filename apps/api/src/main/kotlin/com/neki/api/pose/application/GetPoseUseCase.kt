package com.neki.api.pose.application

import com.neki.api.pose.application.dto.PoseResult
import com.neki.core.annotation.UseCase
import com.neki.core.transaction.TransactionRunner
import com.neki.domain.pose.client.MediaClient
import com.neki.domain.pose.dto.PoseQuery
import com.neki.domain.pose.models.MediaMetadata
import com.neki.domain.pose.service.PoseScrapService
import com.neki.domain.pose.service.PoseViewService

@UseCase
class GetPoseUseCase(
    private val poseScrapService: PoseScrapService,
    private val poseViewService: PoseViewService,
    private val mediaClient: MediaClient,
    private val transactionRunner: TransactionRunner,
) {

    fun execute(query: PoseQuery.GetPose): PoseResult.GetPose {
        val (pose, isScraped) = poseScrapService.getOwnedPoseWithScrap(query)

        if (poseViewService.isFirstViewOf(query)) {
            transactionRunner.run { poseViewService.incrementViewCount(query) }
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
