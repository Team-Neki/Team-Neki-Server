package com.neki.api.pose.application

import com.neki.api.pose.application.dto.PoseAssembler
import com.neki.api.pose.application.dto.PoseResult
import com.neki.core.annotation.UseCase
import com.neki.core.domain.vo.Page
import com.neki.core.transaction.TransactionRunner
import com.neki.domain.pose.client.MediaClient
import com.neki.domain.pose.dto.PoseQuery
import com.neki.domain.pose.models.MediaMetadata
import com.neki.domain.pose.models.Pose
import com.neki.domain.pose.service.PoseService

/**
 * fileName       : GetScrapPosesUseCase
 * author         : darren
 * date           : 2026. 1. 28
 * description    :
 */
@UseCase
class GetScrapPosesUseCase(
    private val poseService: PoseService,
    private val mediaClient: MediaClient,
    private val transactionRunner: TransactionRunner,
) {

    fun execute(query: PoseQuery.GetScrapPoses): PoseResult.GetPoses {
        val page: Page<Pose> = transactionRunner.readOnly {
            poseService.listOwnedScrapPoses(query)
        }

        if (page.items.isEmpty()) {
            return PoseResult.GetPoses(emptyList(), hasNext = false)
        }

        // storageKey 조회 (페이징된 결과에 대해서만)
        val medias: List<MediaMetadata> = mediaClient.getMediaMetadata(
            page.items.map { it.mediaId },
        )

        return PoseResult.GetPoses(PoseAssembler.toScrapItems(page.items, medias), page.hasNext)
    }
}
