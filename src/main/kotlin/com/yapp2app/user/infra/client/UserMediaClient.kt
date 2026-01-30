package com.yapp2app.user.infra.client

import com.yapp2app.media.application.command.ConfirmMediaUploadedCommand
import com.yapp2app.media.application.command.DeleteMediaCommand
import com.yapp2app.media.application.command.GetMediaStorageInfoCommand
import com.yapp2app.media.application.result.ConfirmMediaUploadedResult.UploadConfirmStatus
import com.yapp2app.media.application.usecase.ConfirmMediaUploadedUseCase
import com.yapp2app.media.application.usecase.DeleteMediaUseCase
import com.yapp2app.media.application.usecase.GetMediaStorageInfoUseCase
import com.yapp2app.user.application.contract.MediaAvailability
import com.yapp2app.user.application.port.MediaClientPort
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

    private val log = LoggerFactory.getLogger(javaClass)

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

    override fun verifyMediasUploaded(ownerId: Long, mediaIds: List<Long>): Map<Long, MediaAvailability> {
        if (mediaIds.isEmpty()) return emptyMap()

        val result = confirmMediaUploadedUseCase.execute(
            ConfirmMediaUploadedCommand(ownerId = ownerId, mediaIds = mediaIds),
        )
        return result.results.mapValues { (_, status) ->
            if (status == UploadConfirmStatus.CONFIRMED) MediaAvailability.AVAILABLE else MediaAvailability.UNAVAILABLE
        }
    }

    override fun rollbackMediasUploaded(ownerId: Long, mediaIds: List<Long>) {
        if (mediaIds.isEmpty()) return

        confirmMediaUploadedUseCase.rollback(
            ConfirmMediaUploadedCommand(
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
