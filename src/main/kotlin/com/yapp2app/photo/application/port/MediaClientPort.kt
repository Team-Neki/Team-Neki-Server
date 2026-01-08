package com.yapp2app.photo.application.port

/**
 * fileName       : MediaClient
 * author         : koo
 * date           : 2026. 1. 2. 오후 11:58
 * description    : Media Client 호출을 위한 인터페이스
 */
interface MediaClientPort {

    fun verifyMediaUploaded(ownerId: Long, mediaId: Long): MediaAvailability

    fun getMediaBinaries(ownerId: Long, mediaIds: List<Long>): List<MediaInfo>

    fun deleteMedia(ownerId: Long, mediaId: Long)

    fun deleteMedias(ownerId: Long, mediaIds: List<Long>)

    // contract
    enum class MediaAvailability {
        AVAILABLE,
        UNAVAILABLE,
    }

    data class MediaInfo(val mediaId: Long, val binaryData: ByteArray) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is MediaInfo) return false
            return mediaId == other.mediaId
        }

        override fun hashCode(): Int = mediaId.hashCode()
    }
}
