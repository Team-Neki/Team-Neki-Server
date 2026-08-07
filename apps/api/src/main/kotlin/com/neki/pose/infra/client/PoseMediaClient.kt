package com.neki.pose.infra.client

import com.neki.media.application.ConfirmMediaUploadedUseCase
import com.neki.media.application.GetMediaMetadataListUseCase
import com.neki.media.application.GetMediaMetadataUseCase
import com.neki.media.application.dto.MediaResult
import com.neki.media.dto.MediaCommand
import com.neki.media.dto.MediaQuery
import com.neki.media.models.UploadConfirmStatus
import com.neki.pose.client.MediaClient
import com.neki.pose.models.MediaAvailability
import com.neki.pose.models.MediaMetadata
import org.springframework.stereotype.Component

/**
 * fileName       : PoseMediaClient
 * author         : darren
 * date           : 2026. 1. 27. 17:20
 * description    : monolithic architecture media client
 * - media service 분리 시 OpenFeign, EventPublisher/Consumer로 변경
 */
@Component
class PoseMediaClient(
    private val confirmMediaUploadedUseCase: ConfirmMediaUploadedUseCase,
    private val getMediaMetadataUseCase: GetMediaMetadataUseCase,
    private val getMediaMetadataListUseCase: GetMediaMetadataListUseCase,
) : MediaClient {

    override fun getMediaMetadata(mediaId: Long): MediaMetadata {
        val result: MediaResult.GetMediaMetadata = getMediaMetadataUseCase.execute(
            MediaQuery.GetMediaMetadata(
                ownerId = null,
                mediaId = mediaId,
            ),
        )

        return result.media.toMetadata()
    }

    override fun getMediaMetadata(mediaIds: List<Long>): List<MediaMetadata> {
        val result: MediaResult.GetMediaMetadataList =
            getMediaMetadataListUseCase.execute(MediaQuery.GetMediaMetadataList(null, mediaIds))

        return result.medias.map { it.toMetadata() }
    }

    override fun verifyMediasUploaded(ownerId: Long, mediaIds: List<Long>): Map<Long, MediaAvailability> {
        if (mediaIds.isEmpty()) return emptyMap()

        val result: MediaResult.ConfirmMediasUploaded = confirmMediaUploadedUseCase.execute(
            MediaCommand.ConfirmMediasUploaded(ownerId = ownerId, mediaIds = mediaIds),
        )
        return result.statuses.mapValues { (_, status) ->
            if (status == UploadConfirmStatus.CONFIRMED) {
                MediaAvailability.AVAILABLE
            } else {
                MediaAvailability.UNAVAILABLE
            }
        }
    }

    override fun rollbackMediasUploaded(ownerId: Long, mediaIds: List<Long>) {
        if (mediaIds.isEmpty()) return

        confirmMediaUploadedUseCase.rollback(
            MediaCommand.ConfirmMediasUploaded(
                ownerId = ownerId,
                mediaIds = mediaIds,
            ),
        )
    }

    private fun MediaResult.Metadata.toMetadata(): MediaMetadata = MediaMetadata(
        mediaId = mediaId,
        storageKey = storageKey,
        contentType = contentType,
        width = width,
        height = height,
    )
}
