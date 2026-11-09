package com.neki.api.media.application

import com.neki.api.media.application.dto.MediaResult
import com.neki.core.annotation.UseCase
import com.neki.core.transaction.TransactionRunner
import com.neki.domain.media.dto.MediaCommand
import com.neki.domain.media.models.ImageMetadata
import com.neki.domain.media.models.Media
import com.neki.domain.media.service.MediaDirectUploadService
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * fileName       : StoreMediaDirectlyUseCase
 * author         : darren
 * date           : 2026. 11. 9.
 * description    : 서버가 받은 이미지 바이트를 스토리지에 올리고 UPLOADED media 로 저장한다.
 *                  스토리지 업로드는 트랜잭션 밖에서 하고, DB 저장이 실패하면 올린 객체를 지운다.
 */
@UseCase
class StoreMediaDirectlyUseCase(
    private val mediaDirectUploadService: MediaDirectUploadService,
    private val transactionRunner: TransactionRunner,
) {
    private val log: Logger = LoggerFactory.getLogger(javaClass)

    /**
     * @throws com.neki.core.exception.BusinessException 지원하지 않는 이미지 형식이면 UPLOAD_FAILED
     */
    fun execute(command: MediaCommand.StoreMedia): MediaResult.StoredMedia {
        val metadata: ImageMetadata = mediaDirectUploadService.inspect(command)
        val storageKey: String = mediaDirectUploadService.newStorageKey(command, metadata)

        mediaDirectUploadService.put(storageKey, metadata, command)

        val media: Media = try {
            transactionRunner.run { mediaDirectUploadService.saveUploaded(storageKey, metadata, command) }
        } catch (e: Exception) {
            runCatching { mediaDirectUploadService.deleteStoredObject(storageKey) }
                .onFailure { log.error("[MEDIA_DIRECT_UPLOAD] compensation failed, orphan key={}", storageKey, it) }
            throw e
        }

        return MediaResult.StoredMedia(mediaId = media.id!!, storageKey = media.storageKey)
    }
}
