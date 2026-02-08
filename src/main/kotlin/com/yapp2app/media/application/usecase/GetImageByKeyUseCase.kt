package com.yapp2app.media.application.usecase

import com.yapp2app.common.annotation.UseCase
import com.yapp2app.media.application.command.GetImageByKeyCommand
import com.yapp2app.media.application.port.MediaBinaryCachePort
import com.yapp2app.media.application.port.MediaStoragePort
import com.yapp2app.media.application.result.GetImageByKeyResult

/**
 * fileName       : GetImageByKeyUseCase
 * author         : koo
 * date           : 2026. 1. 21.
 * description    : object key로 이미지 바이너리 조회 (캐시 우선, cache miss 시 S3 조회)
 */
@UseCase
class GetImageByKeyUseCase(private val mediaStorage: MediaStoragePort, private val cache: MediaBinaryCachePort) {

    fun execute(command: GetImageByKeyCommand): GetImageByKeyResult {
        val objectKey = command.objectKey
        val contentType = resolveContentType(objectKey)

        val binaryData = cache.get(objectKey)
            ?: mediaStorage.fetchBinaryByKey(objectKey).also {
                cache.put(objectKey, it)
            }

        return GetImageByKeyResult(
            binaryData = binaryData,
            contentType = contentType,
        )
    }

    private fun resolveContentType(objectKey: String): String {
        val extension = objectKey.substringAfterLast('.', "").lowercase()
        return EXTENSION_TO_CONTENT_TYPE[extension] ?: DEFAULT_CONTENT_TYPE
    }

    companion object {
        private const val DEFAULT_CONTENT_TYPE = "application/octet-stream"

        private val EXTENSION_TO_CONTENT_TYPE = mapOf(
            "jpg" to "image/jpeg",
            "jpeg" to "image/jpeg",
            "png" to "image/png",
            "gif" to "image/gif",
            "webp" to "image/webp",
            "heic" to "image/heic",
            "heif" to "image/heif",
            "svg" to "image/svg+xml",
            "bmp" to "image/bmp",
            "ico" to "image/x-icon",
        )
    }
}
