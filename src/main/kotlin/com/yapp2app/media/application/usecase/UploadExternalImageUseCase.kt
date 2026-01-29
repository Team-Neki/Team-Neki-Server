package com.yapp2app.media.application.usecase

import com.yapp2app.common.annotation.UseCase
import com.yapp2app.media.application.command.UploadExternalImageCommand
import com.yapp2app.media.application.port.ExternalImageFetchPort
import com.yapp2app.media.application.port.MediaRepositoryPort
import com.yapp2app.media.application.port.MediaStoragePort
import com.yapp2app.media.application.result.UploadExternalImageResult
import com.yapp2app.media.domain.MediaKey
import com.yapp2app.media.domain.entity.Media
import org.slf4j.LoggerFactory

/**
 * fileName       : UploadExternalImageUseCase
 * author         : koo
 * date           : 2026. 1. 28.
 * description    : 외부 URL 이미지를 다운로드하여 S3에 저장하는 UseCase
 */
@UseCase
class UploadExternalImageUseCase(
    private val mediaStorage: MediaStoragePort,
    private val mediaRepository: MediaRepositoryPort,
    private val externalImageFetch: ExternalImageFetchPort,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    fun execute(command: UploadExternalImageCommand): UploadExternalImageResult? = try {
        // 이미지 다운로드
        val fetchResult = externalImageFetch.fetch(command.externalUrl)
            ?: throw IllegalStateException("Failed to fetch image from ${command.externalUrl}")

        val (binary, contentType) = fetchResult

        // Storage Key 생성
        val storageKey = MediaKey.generateFromContentType(command.mediaType, contentType)

        // 이미지 업로드
        mediaStorage.uploadBinary(storageKey, binary, contentType)

        val media = Media(
            storageKey = storageKey,
            ownerId = command.ownerId,
            mediaType = command.mediaType,
            contentType = contentType,
        )
        media.markAsUploaded()

        val saved = mediaRepository.save(media)

        UploadExternalImageResult(saved.id!!, storageKey)
    } catch (e: Exception) {
        log.warn("Failed to upload external image: ${command.externalUrl}", e)
        null
    }
}
