package com.yapp2app.photo.api.dto

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
        val folderId: Long?,
        val favorite: Boolean,
        val contentType: String,
        val createdAt: String,
    )
}

data class GetFavoriteSummaryResponse(val latestImageUrl: String?, val totalCount: Long)
