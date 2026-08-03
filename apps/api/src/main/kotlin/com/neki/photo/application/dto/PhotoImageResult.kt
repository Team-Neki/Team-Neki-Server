package com.neki.photo.application.dto

import com.neki.photo.models.UploadMethod
import java.time.LocalDateTime

/**
 * fileName       : PhotoImageResult
 * author         : koo
 * date           : 2026. 7. 21.
 * description    : photo image application 결과
 */
object PhotoImageResult {
    data class GetPhotos(val photos: List<Item>, val hasNext: Boolean, val totalCount: Long) {

        data class Item(
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

    data class GetPhoto(
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

    data class GetFavoriteSummary(val storageKey: String?, val totalCount: Long)
}
