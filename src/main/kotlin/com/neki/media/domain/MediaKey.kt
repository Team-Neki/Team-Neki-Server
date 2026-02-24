package com.neki.media.domain

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

    /**
     * filename에서 확장자를 추출하여 storage key 생성
     * filename에 확장자가 없으면 contentType에서 추출
     */
    fun generate(type: MediaType, filename: String, contentType: String): String {
        val extension = extractExtensionFromFilename(filename)
            ?: extractExtensionFromContentType(contentType)
        return "${type.prefix}/${UUID.randomUUID()}.$extension"
    }

    /**
     * contentType에서 확장자를 추출하여 storage key 생성
     * 외부 URL 이미지 등 filename이 없는 경우 사용
     */
    fun generateFromContentType(type: MediaType, contentType: String): String {
        val extension = extractExtensionFromContentType(contentType)
        return "${type.prefix}/${UUID.randomUUID()}.$extension"
    }

    private fun extractExtensionFromFilename(filename: String): String? {
        val extension = filename.substringAfterLast('.', "")
        return extension.ifBlank { null }
    }

    private fun extractExtensionFromContentType(contentType: String): String =
        CONTENT_TYPE_EXTENSIONS[contentType] ?: contentType.substringAfterLast('/', "")
}
