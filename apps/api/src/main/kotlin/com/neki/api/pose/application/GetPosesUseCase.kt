package com.neki.api.pose.application

import com.neki.api.pose.application.dto.PoseAssembler
import com.neki.api.pose.application.dto.PoseResult
import com.neki.core.annotation.UseCase
import com.neki.core.domain.vo.Page
import com.neki.core.transaction.TransactionRunner
import com.neki.domain.pose.client.MediaClient
import com.neki.domain.pose.dto.PoseQuery
import com.neki.domain.pose.models.MediaMetadata
import com.neki.domain.pose.models.PoseWithScrap
import com.neki.domain.pose.service.PoseService

/**
 * fileName       : GetPosesUseCase
 * author         : darren
 * date           : 2026. 1. 28. 11:31
 * description    : pose 목록 조회
 */
@UseCase
class GetPosesUseCase(
    private val poseService: PoseService,
    private val mediaClient: MediaClient,
    private val transactionRunner: TransactionRunner,
) {

    fun execute(query: PoseQuery.GetPoses): PoseResult.GetPoses {
        val page: Page<PoseWithScrap> = transactionRunner.readOnly {
            poseService.listPosesWithScrap(query)
        }

        if (page.items.isEmpty()) {
            return PoseResult.GetPoses(poses = emptyList(), hasNext = false)
        }

        // storageKey 조회 (페이징된 결과에 대해서만)
        val medias: List<MediaMetadata> = mediaClient.getMediaMetadata(
            page.items.map { it.pose.mediaId },
        )

        return PoseResult.GetPoses(PoseAssembler.toItems(page.items, medias), page.hasNext)
    }
}
