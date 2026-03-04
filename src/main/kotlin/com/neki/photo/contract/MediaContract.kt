package com.neki.photo.contract

/**
 * fileName       : MediaContract
 * author         : koo
 * date           : 2026. 1. 16. 오후 10:30
 * description    :
 */
enum class MediaAvailability {
    AVAILABLE,
    UNAVAILABLE,
}

data class MediaInfo(val mediaId: Long, val contentType: String, val binaryData: ByteArray) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is MediaInfo) return false
        return mediaId == other.mediaId
    }

    override fun hashCode(): Int = mediaId.hashCode()
}

data class MediaStorageInfo(
    val mediaId: Long,
    val storageKey: String,
    val contentType: String,
    val width: Int? = null,
    val height: Int? = null,
)
