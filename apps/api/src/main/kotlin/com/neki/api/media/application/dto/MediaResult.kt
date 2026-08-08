package com.neki.api.media.application.dto

import com.neki.domain.media.models.UploadConfirmStatus
import java.time.Instant

/**
 * fileName       : MediaResult
 * author         : koo
 * date           : 2026. 7. 21.
 * description    : Media domain application result
 */
object MediaResult {
    data class Metadata(
        val mediaId: Long,
        val storageKey: String,
        val contentType: String,
        val width: Int?,
        val height: Int?,
    ) {
        val id: Long
            get() = mediaId
    }

    data class Binary(val media: Metadata, val binaryData: ByteArray) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is Binary) return false
            return media == other.media && binaryData.contentEquals(other.binaryData)
        }

        override fun hashCode(): Int = 31 * media.hashCode() + binaryData.contentHashCode()
    }

    /**
     * 미디어 업로드 티켓 생성 결과
     */
    data class GenerateUploadTicket(val method: String, val expiresAt: Instant, val tickets: List<Item>) {
        data class Item(val mediaId: Long, val uploadUrl: String, val contentType: String)
    }

    data class GetImageByKey(val binaryData: ByteArray, val contentType: String) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is GetImageByKey) return false
            return binaryData.contentEquals(other.binaryData) && contentType == other.contentType
        }

        override fun hashCode(): Int = 31 * binaryData.contentHashCode() + contentType.hashCode()
    }

    data class GetMediaMetadata(val media: Metadata) {
        val id: Long
            get() = media.id

        val storageKey: String
            get() = media.storageKey
    }

    data class GetMediaMetadataList(val medias: List<Metadata>) : List<Metadata> by medias

    data class GetMedias(val medias: List<Binary>) : List<Binary> by medias

    data class ConfirmMediasUploaded(val statuses: Map<Long, UploadConfirmStatus>) :
        Map<Long, UploadConfirmStatus> by statuses

    data class DeleteMedias(val mediaIds: List<Long>)
}
