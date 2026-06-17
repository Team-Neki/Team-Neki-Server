package com.neki.photo.application.result

import com.neki.photo.domain.enums.UploadMethod
import java.time.LocalDateTime

/**
 * fileName       : PhotoImageResult
 * author         : koo
 * date           : 2026. 1. 2. 오후 8:28
 * description    : photo image application 결과
 */
data class GetPhotosResult(val photos: List<PhotoInfo>, val hasNext: Boolean, val totalCount: Long) {

    data class PhotoInfo(
        val photoId: Long,
        val storageKey: String,
        val favorite: Boolean,
        val contentType: String,
        val uploadMethod: UploadMethod?,
        val width: Int? = null,
        val height: Int? = null,
        val memo: String? = null,
        val createdAt: LocalDateTime,
        val capturedAt: LocalDateTime?,
    )
}

data class GetPhotoResult(
    val photoId: Long,
    val storageKey: String,
    val favorite: Boolean,
    val contentType: String,
    val uploadMethod: UploadMethod?,
    val width: Int? = null,
    val height: Int? = null,
    val memo: String? = null,
    val createdAt: LocalDateTime,
    val capturedAt: LocalDateTime?,
)

data class GetFavoriteSummaryResult(val storageKey: String?, val totalCount: Long)
