package com.neki.domain.media.dto

import com.neki.domain.media.models.MediaType

/**
 * fileName       : MediaCommand
 * author         : koo
 * date           : 2026. 7. 21.
 * description    : Media domain command
 */
object MediaCommand {
    data class ConfirmMediasUploaded(val ownerId: Long, val mediaIds: List<Long>)

    data class GenerateUploadTicket(val ownerId: Long, val items: List<Item>) {
        data class Item(
            val filename: String,
            val contentType: String,
            val mediaType: MediaType,
            val width: Int? = null,
            val height: Int? = null,
            val size: Long? = null,
        )
    }

    data class GenerateQrDumpUploadTicket(val ownerId: Long)

    data class DeleteMedias(val ownerId: Long, val mediaIds: List<Long>)

    /**
     * 서버가 바이트를 받아 스토리지에 직접 올린다. 바이트 배열 비교를 피하려고 data class 로 두지 않는다.
     */
    class StoreMedia(val ownerId: Long, val mediaType: MediaType, val content: ByteArray) {
        override fun toString(): String = "StoreMedia(ownerId=$ownerId, mediaType=$mediaType, size=${content.size})"
    }

    data class DiscardMedia(val ownerId: Long, val mediaId: Long)
}
