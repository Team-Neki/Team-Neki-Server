package com.neki.photo.dto

import com.neki.photo.models.UploadMethod
import java.time.LocalDateTime

/**
 * fileName       : PhotoImageCommand
 * author         : koo
 * date           : 2026. 7. 21.
 * description    : Photo image domain command
 */
object PhotoImageCommand {
    data class UploadPhoto(
        override val userId: Long,
        val folderId: Long?,
        val uploads: List<Item>,
        val favorite: Boolean,
    ) : UserScoped {
        data class Item(
            val mediaId: Long,
            val uploadMethod: UploadMethod?,
            val memo: String?,
            val capturedAt: LocalDateTime?,
        )
    }

    data class DeletePhotos(override val userId: Long, val photoIds: List<Long>) : UserScoped

    @Deprecated(message = "PUT API 변경 후 제거")
    data class UpdatePhoto(override val userId: Long, val photoId: Long, val memo: String?) : UserScoped

    data class PutPhoto(
        override val userId: Long,
        val photoId: Long,
        val memo: String?,
        val capturedAt: LocalDateTime?,
    ) : UserScoped

    data class UpdatePhotoFavorite(override val userId: Long, val photoId: Long, val favorite: Boolean) : UserScoped
}
