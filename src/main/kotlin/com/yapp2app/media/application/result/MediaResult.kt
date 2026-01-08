package com.yapp2app.media.application.result

/**
 * fileName       : MediaResult
 * author         : koo
 * date           : 2026. 1. 3. 오전 12:04
 * description    : Media domain application result
 */
data class ConfirmMediaUploadedResult(val success: Boolean)

data class GenerateUploadTicketResult(val mediaId: Long, val presignedUrl: String)

data class GetMediasResult(val medias: List<MediaInfo>) {
    data class MediaInfo(val mediaId: Long, val binaryData: ByteArray, val contentType: String) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is MediaInfo) return false
            return mediaId == other.mediaId
        }

        override fun hashCode(): Int = mediaId.hashCode()
    }
}
