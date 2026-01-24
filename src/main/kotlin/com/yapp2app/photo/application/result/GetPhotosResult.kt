package com.yapp2app.photo.application.result

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
        val folderId: Long?,
        val favorite: Boolean,
        val contentType: String,
        val createdAt: String,
    )
}

data class GetFavoriteSummaryResult(val latestImageUrl: String?, val totalCount: Long)
