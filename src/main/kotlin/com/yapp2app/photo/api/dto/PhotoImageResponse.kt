package com.yapp2app.photo.api.dto

import java.time.LocalDateTime

/**
 * fileName       : PhotoImageResponse
 * author         : koo
 * date           : 2026. 1. 2. 오후 8:28
 * description    : Photo image domain 응답
 */
data class UploadPhotoResponse(val photoId: Long)

data class GetPhotosResponse(val items: List<PhotoInfo>, val hasNext: Boolean) {
    data class PhotoInfo(
        val photoId: Long,
        val imageUrl: String,
        val favorite: Boolean,
        val contentType: String,
        val createdAt: LocalDateTime,
    )
}

data class GetPhotoResponse(
    val photoId: Long,
    val imageUrl: String,
    val favorite: Boolean,
    val contentType: String,
    val createdAt: LocalDateTime,
)

data class GetFavoriteSummaryResponse(val latestImageUrl: String?, val totalCount: Long)
