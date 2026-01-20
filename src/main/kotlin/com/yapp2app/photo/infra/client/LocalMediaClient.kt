package com.yapp2app.photo.infra.client

import com.yapp2app.media.application.command.ConfirmMediaUploadedCommand
import com.yapp2app.media.application.command.DeleteMediaCommand
import com.yapp2app.media.application.command.DeleteMediasCommand
import com.yapp2app.media.application.command.GetMediaStorageInfosCommand
import com.yapp2app.media.application.command.GetMediasCommand
import com.yapp2app.media.application.usecase.ConfirmMediaUploadedUseCase
import com.yapp2app.media.application.usecase.DeleteMediaUseCase
import com.yapp2app.media.application.usecase.GetMediaStorageInfosUseCase
import com.yapp2app.media.application.usecase.GetMediasUseCase
import com.yapp2app.photo.application.contract.MediaAvailability
import com.yapp2app.photo.application.contract.MediaInfo
import com.yapp2app.photo.application.contract.MediaStorageInfo
import com.yapp2app.photo.application.port.MediaClientPort
import com.yapp2app.photo.application.port.MediaClientPort.*
import org.springframework.stereotype.Component

/**
 * fileName       : LocalMediaClient
 * author         : koo
 * date           : 2026. 1. 3. 오전 12:00
 * description    : monolithic architecture media client
 * - media service 분리 시 OpenFeign, EventPublisher/Consumer로 변경
 */
@Component
class LocalMediaClient(
    private val confirmMediaUploadedUseCase: ConfirmMediaUploadedUseCase,
    private val getMediasUseCase: GetMediasUseCase,
    private val getMediaStorageInfosUseCase: GetMediaStorageInfosUseCase,
    private val deleteMediaUseCase: DeleteMediaUseCase,
) : MediaClientPort {

    override fun verifyMediaUploaded(ownerId: Long, mediaId: Long): MediaAvailability {
        val result = confirmMediaUploadedUseCase.execute(
            ConfirmMediaUploadedCommand(
                ownerId = ownerId,
                mediaId = mediaId,
            ),
        )

        return if (result.success) {
            MediaAvailability.AVAILABLE
        } else {
            MediaAvailability.UNAVAILABLE
        }
    }

    override fun getMediaBinaries(ownerId: Long, mediaIds: List<Long>): List<MediaInfo> {
        val result = getMediasUseCase.execute(GetMediasCommand(ownerId, mediaIds))

        return result.medias.map {
            MediaInfo(
                mediaId = it.mediaId,
                contentType = it.contentType,
                binaryData = it.binaryData,
            )
        }.toList()
    }

    override fun getMediaStorageInfos(ownerId: Long, mediaIds: List<Long>): List<MediaStorageInfo> {
        val result = getMediaStorageInfosUseCase.execute(GetMediaStorageInfosCommand(ownerId, mediaIds))

        return result.storageInfos.map {
            MediaStorageInfo(
                mediaId = it.mediaId,
                storageKey = it.storageKey,
                contentType = it.contentType,
            )
        }
    }

    override fun deleteMedia(ownerId: Long, mediaId: Long) {
        deleteMediaUseCase.execute(DeleteMediaCommand(ownerId, mediaId))
    }

    override fun deleteMedias(ownerId: Long, mediaIds: List<Long>) {
        deleteMediaUseCase.execute(DeleteMediasCommand(ownerId, mediaIds))
    }

    override fun rollbackMediaUploaded(ownerId: Long, mediaId: Long) {
        confirmMediaUploadedUseCase.rollback(
            ConfirmMediaUploadedCommand(ownerId = ownerId, mediaId = mediaId),
        )
    }
}
