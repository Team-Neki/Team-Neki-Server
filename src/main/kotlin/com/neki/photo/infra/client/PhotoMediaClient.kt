package com.neki.photo.infra.client

import com.neki.media.application.command.ConfirmMediasUploadedCommand
import com.neki.media.application.command.DeleteMediasCommand
import com.neki.media.application.command.GetMediaStorageInfoCommand
import com.neki.media.application.command.GetMediaStorageInfosCommand
import com.neki.media.application.command.GetMediasCommand
import com.neki.media.application.result.ConfirmMediasUploadedResult.UploadConfirmStatus
import com.neki.media.application.result.GetMediaStorageInfoResult
import com.neki.media.application.usecase.ConfirmMediaUploadedUseCase
import com.neki.media.application.usecase.DeleteMediaUseCase
import com.neki.media.application.usecase.GetMediaStorageInfoUseCase
import com.neki.media.application.usecase.GetMediaStorageInfosUseCase
import com.neki.media.application.usecase.GetMediasUseCase
import com.neki.photo.contract.MediaAvailability
import com.neki.photo.contract.MediaInfo
import com.neki.photo.contract.MediaStorageInfo
import com.neki.photo.application.port.MediaClientPort
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
    private val getMediasUseCase: GetMediasUseCase,
    private val getMediaStorageInfoUseCase: GetMediaStorageInfoUseCase,
    private val getMediaStorageInfosUseCase: GetMediaStorageInfosUseCase,
    private val deleteMediaUseCase: DeleteMediaUseCase,
) : MediaClientPort {

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

    override fun getMediaStorageInfo(ownerId: Long, mediaId: Long): MediaStorageInfo {
        val result: GetMediaStorageInfoResult = getMediaStorageInfoUseCase.execute(
            GetMediaStorageInfoCommand(
                ownerId = ownerId,
                mediaId = mediaId,
            ),
        )

        return MediaStorageInfo(
            mediaId = result.mediaId,
            storageKey = result.storageKey,
            contentType = result.contentType,
            width = result.width,
            height = result.height,
        )
    }

    override fun getMediaStorageInfos(ownerId: Long, mediaIds: List<Long>): List<MediaStorageInfo> {
        val result =
            getMediaStorageInfosUseCase.execute(GetMediaStorageInfosCommand(ownerId, mediaIds))

        return result.storageInfos.map {
            MediaStorageInfo(
                mediaId = it.mediaId,
                storageKey = it.storageKey,
                contentType = it.contentType,
                width = it.width,
                height = it.height,
            )
        }
    }

    override fun deleteMedias(ownerId: Long, mediaIds: List<Long>) {
        deleteMediaUseCase.execute(DeleteMediasCommand(ownerId, mediaIds))
    }

    override fun verifyMediasUploaded(ownerId: Long, mediaIds: List<Long>): Map<Long, MediaAvailability> {
        if (mediaIds.isEmpty()) return emptyMap()

        val result = confirmMediaUploadedUseCase.execute(
            ConfirmMediasUploadedCommand(ownerId = ownerId, mediaIds = mediaIds),
        )
        return result.results.mapValues { (_, status) ->
            if (status == UploadConfirmStatus.CONFIRMED) MediaAvailability.AVAILABLE else MediaAvailability.UNAVAILABLE
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
}
