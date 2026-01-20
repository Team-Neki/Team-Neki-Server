package com.yapp2app.media.domain

import java.util.UUID

/**
 * fileName       : MediaKey
 * author         : koo
 * date           : 2025. 12. 19. 오전 2:41
 * description    : 이미지 저장을 위한 key
 */
object MediaKey {

    private val CONTENT_TYPE_EXTENSIONS = mapOf(
        "image/jpeg" to "jpg",
        "image/png" to "png",
        "image/gif" to "gif",
        "image/webp" to "webp",
        "image/heic" to "heic",
        "image/heif" to "heif",
    )

    fun generate(type: MediaType, filename: String, contentType: String): String {
        val extension = extractExtension(filename, contentType)
        return "${type.prefix}/${UUID.randomUUID()}.$extension"
    }

    private fun extractExtension(filename: String, contentType: String): String {
        val filenameExtension = filename.substringAfterLast('.', "")
        if (filenameExtension.isNotBlank()) {
            return filenameExtension
        }
        return CONTENT_TYPE_EXTENSIONS[contentType]
            ?: contentType.substringAfterLast('/', "")
    }
}
