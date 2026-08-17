package com.neki.api.pose.application.dto

import com.neki.domain.pose.models.MediaMetadata
import com.neki.domain.pose.models.MediaMetadatas
import com.neki.domain.pose.models.Pose
import com.neki.domain.pose.models.PoseWithScrap
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * fileName       : PoseAssembler
 * author         : koo
 * date           : 2026. 8. 4.
 * description    : 포즈에 media 메타데이터를 붙여 응답 항목으로 조립한다.
 */
object PoseAssembler {

    private val log: Logger = LoggerFactory.getLogger(javaClass)

    /**
     * 아직 media가 저장되지 않은 포즈는 제외한다 (eventually consistent).
     */
    fun toItems(poses: List<PoseWithScrap>, medias: List<MediaMetadata>): List<PoseResult.GetPoses.Item> {
        val metadatas = MediaMetadatas(medias)

        return poses.mapNotNull { (pose, isScraped) ->
            val media: MediaMetadata = metadatas[pose.mediaId]
                ?: run {
                    log.info("Media not found yet. photoId={}, fileId={}", pose.id, pose.mediaId)
                    return@mapNotNull null
                }

            PoseResult.GetPoses.Item(
                poseId = pose.id!!,
                headCount = pose.headCount,
                storageKey = media.storageKey,
                scrap = isScraped,
                contentType = media.contentType,
                width = media.width,
                height = media.height,
                createdAt = pose.createdAt!!,
            )
        }
    }

    /**
     * 스크랩 목록은 전부 scrap = true 다.
     */
    fun toScrapItems(poses: List<Pose>, medias: List<MediaMetadata>): List<PoseResult.GetPoses.Item> =
        toItems(poses.map { PoseWithScrap(it, true) }, medias)
}
