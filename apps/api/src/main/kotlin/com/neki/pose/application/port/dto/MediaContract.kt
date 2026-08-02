package com.neki.pose.application.port.dto

/**
 * fileName       : MediaContract
 * author         : koo
 * date           : 2026. 7. 22.
 * description    : MediaClientPort 계약 타입
 */
object MediaContract {
    enum class Availability {
        AVAILABLE,
        UNAVAILABLE,
    }

    data class Info(val mediaId: Long, val contentType: String, val binaryData: ByteArray) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is Info) return false
            return mediaId == other.mediaId
        }

        override fun hashCode(): Int = mediaId.hashCode()
    }

    data class StorageInfo(
        val mediaId: Long,
        val storageKey: String,
        val contentType: String,
        val width: Int? = null,
        val height: Int? = null,
    )
}
