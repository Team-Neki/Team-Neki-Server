package com.neki.photo.application.dto

import com.neki.photo.domain.enums.UploadMethod
import java.time.LocalDateTime

/**
 * fileName       : PhotoImageCommand
 * author         : koo
 * date           : 2026. 7. 21.
 * description    : Photo image domain command
 */
object PhotoImageCommand {
    data class UploadPhoto(
        val userId: Long,
        val folderId: Long?,
        val uploads: List<UploadItem>,
        val favorite: Boolean,
    ) {
        data class UploadItem(
            val mediaId: Long,
            val uploadMethod: UploadMethod?,
            val memo: String?,
            val capturedAt: LocalDateTime?,
        )
    }

    data class DeletePhotos(val userId: Long, val photoIds: List<Long>)

    @Deprecated(message = "PUT API 변경 후 제거")
    data class UpdatePhoto(val userId: Long, val photoId: Long, val memo: String?)

    data class PutPhoto(val userId: Long, val photoId: Long, val memo: String?, val capturedAt: LocalDateTime?)

    data class UpdatePhotoFavorite(val userId: Long, val photoId: Long, val favorite: Boolean)
}
