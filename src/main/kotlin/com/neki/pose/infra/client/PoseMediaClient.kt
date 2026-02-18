package com.neki.pose.infra.client

import com.neki.media.application.command.ConfirmMediasUploadedCommand
import com.neki.media.application.command.GetMediaStorageInfoCommand
import com.neki.media.application.command.GetMediaStorageInfosCommand
import com.neki.media.application.result.ConfirmMediasUploadedResult.UploadConfirmStatus
import com.neki.media.application.result.GetMediaStorageInfoResult
import com.neki.media.application.usecase.ConfirmMediaUploadedUseCase
import com.neki.media.application.usecase.GetMediaStorageInfoUseCase
import com.neki.media.application.usecase.GetMediaStorageInfosUseCase
import com.neki.pose.application.contract.MediaAvailability
import com.neki.pose.application.contract.MediaStorageInfo
import com.neki.pose.application.port.MediaClientPort
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
    private val getMediaStorageInfoUseCase: GetMediaStorageInfoUseCase,
    private val getMediaStorageInfosUseCase: GetMediaStorageInfosUseCase,
) : MediaClientPort {

    override fun getMediaStorageInfo(mediaId: Long): MediaStorageInfo {
        val result: GetMediaStorageInfoResult = getMediaStorageInfoUseCase.execute(
            GetMediaStorageInfoCommand(
                ownerId = null,
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

    override fun getMediaStorageInfos(mediaIds: List<Long>): List<MediaStorageInfo> {
        val result =
            getMediaStorageInfosUseCase.execute(GetMediaStorageInfosCommand(null, mediaIds))

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
