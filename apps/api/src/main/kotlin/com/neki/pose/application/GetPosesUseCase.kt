package com.neki.pose.application

import com.neki.common.annotation.UseCase
import com.neki.common.domain.vo.Page
import com.neki.common.transaction.TransactionRunner
import com.neki.pose.MediaClient
import com.neki.pose.application.dto.PoseAssembler
import com.neki.pose.application.dto.PoseResult
import com.neki.pose.dto.PoseQuery
import com.neki.pose.models.MediaMetadata
import com.neki.pose.models.PoseWithScrap
import com.neki.pose.service.PoseService

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
