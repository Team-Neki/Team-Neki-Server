package com.neki.api.pose.infra.client

import com.neki.api.media.application.ConfirmMediaUploadedUseCase
import com.neki.api.media.application.DiscardMediaUseCase
import com.neki.api.media.application.GetMediaMetadataListUseCase
import com.neki.api.media.application.GetMediaMetadataUseCase
import com.neki.api.media.application.StoreMediaDirectlyUseCase
import com.neki.api.media.application.dto.MediaResult
import com.neki.core.code.ResultCode
import com.neki.core.exception.BusinessException
import com.neki.domain.media.dto.MediaCommand
import com.neki.domain.media.dto.MediaQuery
import com.neki.domain.media.models.MediaType
import com.neki.domain.media.models.UploadConfirmStatus
import com.neki.domain.pose.client.MediaClient
import com.neki.domain.pose.models.BulkFailReason
import com.neki.domain.pose.models.MediaAvailability
import com.neki.domain.pose.models.MediaMetadata
import com.neki.domain.pose.models.PoseImageFile
import com.neki.domain.pose.models.PoseImageStoreOutcome
import org.slf4j.Logger
import org.slf4j.LoggerFactory
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
    private val storeMediaDirectlyUseCase: StoreMediaDirectlyUseCase,
    private val discardMediaUseCase: DiscardMediaUseCase,
) : MediaClient {
    private val log: Logger = LoggerFactory.getLogger(javaClass)

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

    override fun storePoseImage(ownerId: Long, image: PoseImageFile): PoseImageStoreOutcome = try {
        val result: MediaResult.StoredMedia = storeMediaDirectlyUseCase.execute(
            MediaCommand.StoreMedia(ownerId = ownerId, mediaType = MediaType.POSE, content = image.readBytes()),
        )
        PoseImageStoreOutcome.Stored(result.mediaId)
    } catch (e: BusinessException) {
        if (e.resultCode == ResultCode.UPLOAD_FAILED) {
            log.warn("[POSE_BULK_UPLOAD] unsupported image file={}", image.fileName)
            PoseImageStoreOutcome.Failed(BulkFailReason.UNSUPPORTED_IMAGE)
        } else {
            log.error("[POSE_BULK_UPLOAD] store failed file={}", image.fileName, e)
            PoseImageStoreOutcome.Failed(BulkFailReason.STORAGE_FAILED)
        }
    } catch (e: Exception) {
        log.error("[POSE_BULK_UPLOAD] store failed file={}", image.fileName, e)
        PoseImageStoreOutcome.Failed(BulkFailReason.STORAGE_FAILED)
    }

    override fun discardPoseImage(ownerId: Long, mediaId: Long) {
        discardMediaUseCase.execute(MediaCommand.DiscardMedia(ownerId = ownerId, mediaId = mediaId))
    }

    private fun MediaResult.Metadata.toMetadata(): MediaMetadata = MediaMetadata(
        mediaId = mediaId,
        storageKey = storageKey,
        contentType = contentType,
        width = width,
        height = height,
    )
}
