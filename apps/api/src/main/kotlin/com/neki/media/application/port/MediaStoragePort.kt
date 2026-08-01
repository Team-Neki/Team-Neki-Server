package com.neki.media.application.port

import com.neki.media.application.dto.MediaRef
import com.neki.media.application.port.dto.MediaStorageContract

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

    fun generateUploadTicket(key: String, contentType: String): MediaStorageContract.UploadTicket
}
