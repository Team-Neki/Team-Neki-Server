package com.neki.pose.application

import com.neki.common.annotation.UseCase
import com.neki.pose.MediaClient
import com.neki.pose.application.dto.PoseResult
import com.neki.pose.dto.PoseQuery
import com.neki.pose.models.MediaMetadata
import com.neki.pose.models.Pose
import com.neki.pose.service.PoseService

@UseCase
class RandomPoseUseCase(private val poseService: PoseService, private val mediaClient: MediaClient) {

    fun execute(query: PoseQuery.GetRandomPose): PoseResult.GetPose {
        val pose: Pose = poseService.pickRandomPose(query)

        val isScraped: Boolean = poseService.isScraped(query, pose)

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
