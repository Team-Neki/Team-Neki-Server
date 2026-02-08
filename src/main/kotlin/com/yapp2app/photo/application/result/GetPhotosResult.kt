package com.yapp2app.photo.application.result

import java.time.LocalDateTime

/**
 * fileName       : PhotoImageResult
 * author         : koo
 * date           : 2026. 1. 2. 오후 8:28
 * description    : photo image application 결과
 */
data class GetPhotosResult(val photos: List<PhotoInfo>, val hasNext: Boolean) {

    data class PhotoInfo(
        val photoId: Long,
        val storageKey: String,
        val favorite: Boolean,
        val contentType: String,
        val createdAt: LocalDateTime,
    )
}

data class GetPhotoResult(
    val photoId: Long,
    val storageKey: String,
    val favorite: Boolean,
    val contentType: String,
    val createdAt: LocalDateTime,
)

data class GetFavoriteSummaryResult(val storageKey: String?, val totalCount: Long)
