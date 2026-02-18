package com.neki.media.application.result

import java.time.Instant

/**
 * fileName       : MediaResult
 * author         : koo
 * date           : 2026. 1. 3. 오전 12:04
 * description    : Media domain application result
 */
/**
 * 미디어 업로드 확인 결과
 */
data class ConfirmMediasUploadedResult(val results: Map<Long, UploadConfirmStatus>) {
    enum class UploadConfirmStatus { CONFIRMED, NOT_FOUND, NOT_UPLOADED }
}

/**
 * 미디어 업로드 티켓 생성 결과
 */
data class GenerateUploadTicketResult(
    val method: String,
    val expiresAt: Instant,
    val tickets: List<UploadTicketInfo>,
) {
    data class UploadTicketInfo(val mediaId: Long, val uploadUrl: String, val contentType: String)
}

/**
 * 미디어 조회
 */
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

data class GetImageByKeyResult(val binaryData: ByteArray, val contentType: String) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is GetImageByKeyResult) return false
        return binaryData.contentEquals(other.binaryData) && contentType == other.contentType
    }

    override fun hashCode(): Int = 31 * binaryData.contentHashCode() + contentType.hashCode()
}

data class GetMediaStorageInfoResult(
    val mediaId: Long,
    val storageKey: String,
    val contentType: String,
    val width: Int? = null,
    val height: Int? = null,
)

data class GetMediaStorageInfosResult(val storageInfos: List<StorageInfo>) {
    data class StorageInfo(
        val mediaId: Long,
        val storageKey: String,
        val contentType: String,
        val width: Int? = null,
        val height: Int? = null,
    )
}
