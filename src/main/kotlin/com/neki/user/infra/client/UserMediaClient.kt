package com.neki.user.infra.client

import com.neki.media.application.command.ConfirmMediasUploadedCommand
import com.neki.media.application.command.DeleteMediaCommand
import com.neki.media.application.command.GetMediaStorageInfoCommand
import com.neki.media.application.result.ConfirmMediasUploadedResult
import com.neki.media.application.usecase.ConfirmMediaUploadedUseCase
import com.neki.media.application.usecase.DeleteMediaUseCase
import com.neki.media.application.usecase.GetMediaStorageInfoUseCase
import com.neki.user.application.contract.MediaAvailability
import com.neki.user.application.port.MediaClientPort
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
    private val getMediaStorageInfoUseCase: GetMediaStorageInfoUseCase,
) : MediaClientPort {

    private val log: Logger = LoggerFactory.getLogger(javaClass)

    override fun deleteMedia(ownerId: Long, mediaId: Long) {
        runCatching {
            deleteMediaUseCase.execute(DeleteMediaCommand(ownerId, mediaId))
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
        val result: ConfirmMediasUploadedResult = confirmMediaUploadedUseCase.execute(
            ConfirmMediasUploadedCommand(
                ownerId = ownerId,
                mediaIds = listOf(mediaId),
            ),
        )

        return when (result.results[mediaId]) {
            ConfirmMediasUploadedResult.UploadConfirmStatus.CONFIRMED -> MediaAvailability.AVAILABLE
            else -> MediaAvailability.UNAVAILABLE
        }
    }

    override fun rollbackMediasUploaded(ownerId: Long, mediaIds: List<Long>) {
        if (mediaIds.isEmpty()) return

        confirmMediaUploadedUseCase.rollback(
            ConfirmMediasUploadedCommand(
                ownerId = ownerId,
                mediaIds = mediaIds,
            ),
        )
    }

    override fun getStorageKey(ownerId: Long, mediaId: Long): String? = runCatching {
        getMediaStorageInfoUseCase.execute(
            GetMediaStorageInfoCommand(ownerId = ownerId, mediaId = mediaId),
        ).storageKey
    }.getOrNull()
}
