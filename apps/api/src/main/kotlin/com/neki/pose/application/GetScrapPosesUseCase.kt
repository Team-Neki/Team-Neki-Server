package com.neki.pose.application

import com.neki.common.annotation.UseCase
import com.neki.common.domain.vo.Page
import com.neki.common.transaction.TransactionRunner
import com.neki.pose.MediaClient
import com.neki.pose.application.dto.PoseAssembler
import com.neki.pose.application.dto.PoseResult
import com.neki.pose.dto.PoseQuery
import com.neki.pose.models.MediaMetadata
import com.neki.pose.models.Pose
import com.neki.pose.service.PoseService

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
