package com.yapp2app.pose.application.contract

/**
 * fileName       : MediaAvailability
 * author         : darren
 * date           : 2026. 1. 29. 11:19
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

data class MediaStorageInfo(val mediaId: Long, val storageKey: String, val contentType: String)
