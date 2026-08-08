package com.neki.api.photo.infra.client

import com.neki.api.media.application.ConfirmMediaUploadedUseCase
import com.neki.api.media.application.DeleteMediaUseCase
import com.neki.api.media.application.GetMediaMetadataListUseCase
import com.neki.api.media.application.GetMediaMetadataUseCase
import com.neki.api.media.application.dto.MediaResult
import com.neki.domain.media.dto.MediaCommand
import com.neki.domain.media.dto.MediaQuery
import com.neki.domain.media.models.UploadConfirmStatus
import com.neki.domain.photo.client.MediaClient
import com.neki.domain.photo.models.MediaAvailability
import com.neki.domain.photo.models.MediaMetadata
import org.springframework.stereotype.Component

/**
 * fileName       : PhotoMediaClient
 * author         : koo
 * date           : 2026. 1. 3. 오전 12:00
 * description    : monolithic architecture media client
 * - media service 분리 시 OpenFeign, EventPublisher/Consumer로 변경
 */
@Component
class PhotoMediaClient(
    private val confirmMediaUploadedUseCase: ConfirmMediaUploadedUseCase,
    private val getMediaMetadataUseCase: GetMediaMetadataUseCase,
    private val getMediaMetadataListUseCase: GetMediaMetadataListUseCase,
    private val deleteMediaUseCase: DeleteMediaUseCase,
) : MediaClient {

    override fun getMediaMetadata(ownerId: Long, mediaId: Long): MediaMetadata {
        val result: MediaResult.GetMediaMetadata = getMediaMetadataUseCase.execute(
            MediaQuery.GetMediaMetadata(
                ownerId = ownerId,
                mediaId = mediaId,
            ),
        )

        return result.media.toMetadata()
    }

    override fun getMediaMetadata(ownerId: Long, mediaIds: List<Long>): List<MediaMetadata> {
        val result: MediaResult.GetMediaMetadataList =
            getMediaMetadataListUseCase.execute(MediaQuery.GetMediaMetadataList(ownerId, mediaIds))

        return result.medias.map { it.toMetadata() }
    }

    override fun deleteMedias(ownerId: Long, mediaIds: List<Long>) {
        deleteMediaUseCase.execute(MediaCommand.DeleteMedias(ownerId, mediaIds))
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
