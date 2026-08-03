package com.neki.media.models

/**
 * fileName       : MediaBinary
 * author         : koo
 * date           : 2026. 8. 3.
 * description    : 미디어와 해당 바이너리 데이터 쌍
 */
data class MediaBinary(val media: Media, val binaryData: ByteArray) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is MediaBinary) return false
        return media.id == other.media.id
    }

    override fun hashCode(): Int = media.id.hashCode()
}
