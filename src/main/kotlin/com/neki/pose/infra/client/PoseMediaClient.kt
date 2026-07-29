package com.neki.pose.infra.client

import com.neki.media.application.dto.MediaCommand
import com.neki.media.application.dto.MediaQuery
import com.neki.media.application.dto.MediaResult
import com.neki.media.application.dto.MediaResult.ConfirmMediasUploaded.UploadConfirmStatus
import com.neki.media.application.usecase.ConfirmMediaUploadedUseCase
import com.neki.media.application.usecase.GetMediaStorageInfoUseCase
import com.neki.media.application.usecase.GetMediaStorageInfosUseCase
import com.neki.pose.application.port.MediaClientPort
import com.neki.pose.application.port.dto.MediaContract
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

    override fun getMediaStorageInfo(mediaId: Long): MediaContract.StorageInfo {
        val result: MediaResult.GetMediaStorageInfo = getMediaStorageInfoUseCase.execute(
            MediaQuery.GetMediaStorageInfo(
                ownerId = null,
                mediaId = mediaId,
            ),
        )

        return MediaContract.StorageInfo(
            mediaId = result.mediaId,
            storageKey = result.storageKey,
            contentType = result.contentType,
            width = result.width,
            height = result.height,
        )
    }

    override fun getMediaStorageInfos(mediaIds: List<Long>): List<MediaContract.StorageInfo> {
        val result: MediaResult.GetMediaStorageInfos =
            getMediaStorageInfosUseCase.execute(MediaQuery.GetMediaStorageInfos(null, mediaIds))

        return result.storageInfos.map {
            MediaContract.StorageInfo(
                mediaId = it.mediaId,
                storageKey = it.storageKey,
                contentType = it.contentType,
                width = it.width,
                height = it.height,
            )
        }
    }

    override fun verifyMediasUploaded(ownerId: Long, mediaIds: List<Long>): Map<Long, MediaContract.Availability> {
        if (mediaIds.isEmpty()) return emptyMap()

        val result: MediaResult.ConfirmMediasUploaded = confirmMediaUploadedUseCase.execute(
            MediaCommand.ConfirmMediasUploaded(ownerId = ownerId, mediaIds = mediaIds),
        )
        return result.results.mapValues { (_, status) ->
            if (status == UploadConfirmStatus.CONFIRMED) {
                MediaContract.Availability.AVAILABLE
            } else {
                MediaContract.Availability.UNAVAILABLE
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
}
