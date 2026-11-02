package com.neki.api.pose.application

import com.neki.api.pose.application.dto.PoseResult
import com.neki.core.annotation.UseCase
import com.neki.domain.pose.client.MediaClient
import com.neki.domain.pose.dto.PoseQuery
import com.neki.domain.pose.models.MediaMetadata
import com.neki.domain.pose.service.PoseScrapService

@UseCase
class GetPoseUseCase(private val poseScrapService: PoseScrapService, private val mediaClient: MediaClient) {

    fun execute(query: PoseQuery.GetPose): PoseResult.GetPose {
        val (pose, isScraped) = poseScrapService.getOwnedPoseWithScrap(query)

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
