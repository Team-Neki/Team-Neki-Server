package com.neki.api.user.infra.client

import com.neki.api.media.application.ConfirmMediaUploadedUseCase
import com.neki.api.media.application.DeleteMediaUseCase
import com.neki.api.media.application.GetMediaMetadataUseCase
import com.neki.api.media.application.dto.MediaResult
import com.neki.domain.media.dto.MediaCommand
import com.neki.domain.media.dto.MediaQuery
import com.neki.domain.media.models.UploadConfirmStatus
import com.neki.domain.user.client.MediaClient
import com.neki.domain.user.models.MediaAvailability
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

/**
 * fileName       : UserMediaClient
 * author         : koo
 * date           : 2026. 1. 28. 오후 3:59
 * description    :
 */
@Component
class UserMediaClient(
    private val deleteMediaUseCase: DeleteMediaUseCase,
    private val confirmMediaUploadedUseCase: ConfirmMediaUploadedUseCase,
    private val getMediaMetadataUseCase: GetMediaMetadataUseCase,
) : MediaClient {

    private val log: Logger = LoggerFactory.getLogger(javaClass)

    override fun deleteMedia(ownerId: Long, mediaId: Long) {
        runCatching {
            deleteMediaUseCase.execute(MediaCommand.DeleteMedias(ownerId, listOf(mediaId)))
        }.onFailure { e ->
            log.warn(
                "Failed to request media deletion. Will be cleaned up by batch later. ownerId={}, mediaId={}",
                ownerId,
                mediaId,
                e,
            )
        }
    }

    override fun verifyMediaUploaded(ownerId: Long, mediaId: Long): MediaAvailability {
        val result: MediaResult.ConfirmMediasUploaded = confirmMediaUploadedUseCase.execute(
            MediaCommand.ConfirmMediasUploaded(
                ownerId = ownerId,
                mediaIds = listOf(mediaId),
            ),
        )

        return when (result.statuses[mediaId]) {
            UploadConfirmStatus.CONFIRMED -> MediaAvailability.AVAILABLE
            else -> MediaAvailability.UNAVAILABLE
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

    override fun getStorageKey(ownerId: Long, mediaId: Long): String? = runCatching {
        getMediaMetadataUseCase.execute(
            MediaQuery.GetMediaMetadata(ownerId = ownerId, mediaId = mediaId),
        ).media.storageKey
    }.getOrNull()
}
