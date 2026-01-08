package com.yapp2app.media.application.port

import com.yapp2app.media.application.dto.MediaRef
import java.time.Instant

/**
 * fileName       : MediaStorage
 * author         : koo
 * date           : 2025. 12. 19. 오전 2:41
 * description    : 이미지 저장을 위한 인터페이스 (port)
 */
interface MediaStoragePort {

    fun deleteByKey(key: String)

    fun findByKey(key: String): String

    fun fetchBinaryByKey(key: String): ByteArray

    fun findAll(prefix: String): List<MediaRef>

    fun exists(key: String): Boolean

    fun generateUploadTicket(key: String, contentType: String): UploadTicket

    // contract
    data class UploadTicket(val url: String, val method: String, val expiresAt: Instant, val contentType: String)
}
