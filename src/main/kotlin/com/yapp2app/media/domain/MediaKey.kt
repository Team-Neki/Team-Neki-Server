package com.yapp2app.media.domain

import com.yapp2app.media.domain.MediaType
import java.util.UUID

/**
 * fileName       : MediaKey
 * author         : koo
 * date           : 2025. 12. 19. 오전 2:41
 * description    : 이미지 저장을 위한 key
 */
object MediaKey {

    fun generate(type: MediaType, filename: String): String {
        val extension = filename.substringAfterLast('.', "")

        return "${type.prefix}/${UUID.randomUUID()}.$extension"
    }
}
