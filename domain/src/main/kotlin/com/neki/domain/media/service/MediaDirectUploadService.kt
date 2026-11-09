package com.neki.domain.media.service

import com.neki.core.code.ResultCode
import com.neki.core.exception.BusinessException
import com.neki.domain.media.dto.MediaCommand
import com.neki.domain.media.external.MediaStorage
import com.neki.domain.media.models.ImageHeaderParser
import com.neki.domain.media.models.ImageMetadata
import com.neki.domain.media.models.Media
import com.neki.domain.media.models.MediaKey
import com.neki.domain.media.models.MediaStatus
import com.neki.domain.media.repository.MediaRepository
import org.springframework.stereotype.Component

/**
 * fileName       : MediaDirectUploadService
 * author         : darren
 * date           : 2026. 11. 9.
 * description    : 서버가 바이트를 받아 스토리지에 직접 올리는 경로 (관리자 일괄 업로드).
 *                  presigned URL 흐름([MediaService])과 달리 형식과 크기를 서버가 바이트에서 직접 확인한다.
 */
@Component
class MediaDirectUploadService(private val mediaRepository: MediaRepository, private val mediaStorage: MediaStorage) {

    /**
     * 형식은 Content-Type 헤더나 확장자가 아니라 바이트 헤더(magic byte)로 판정한다.
     */
    fun inspect(command: MediaCommand.StoreMedia): ImageMetadata =
        ImageHeaderParser.parse(command.content) ?: throw BusinessException(ResultCode.UPLOAD_FAILED)

    /**
     * 확장자는 감지한 형식을 따른다. 업로드 파일명은 쓰지 않는다.
     */
    fun newStorageKey(command: MediaCommand.StoreMedia, metadata: ImageMetadata): String =
        MediaKey.generate(command.mediaType, "image.${metadata.extension}", metadata.contentType)

    fun put(storageKey: String, metadata: ImageMetadata, command: MediaCommand.StoreMedia) {
        mediaStorage.put(storageKey, metadata.contentType, command.content)
    }

    fun saveUploaded(storageKey: String, metadata: ImageMetadata, command: MediaCommand.StoreMedia): Media =
        mediaRepository.save(
            Media(
                storageKey = storageKey,
                ownerId = command.ownerId,
                mediaType = command.mediaType,
                status = MediaStatus.UPLOADED,
                contentType = metadata.contentType,
                width = metadata.width,
                height = metadata.height,
                size = command.content.size.toLong(),
            ),
        )

    /**
     * 스토리지 객체를 지우는 보상 경로. DB 저장이 실패했을 때 쓴다.
     */
    fun deleteStoredObject(storageKey: String) {
        mediaStorage.deleteByKey(storageKey)
    }

    /**
     * 직접 올린 media 를 되돌린다. 스토리지 객체를 지우고 DELETED 로 표시한다.
     */
    fun discard(command: MediaCommand.DiscardMedia): Media {
        val media: Media = mediaRepository.getActiveMedia(command.ownerId, command.mediaId)
            ?: throw BusinessException(ResultCode.NOT_FOUND)

        mediaStorage.deleteByKey(media.storageKey)
        media.markAsDeleted()
        return mediaRepository.save(media)
    }
}
