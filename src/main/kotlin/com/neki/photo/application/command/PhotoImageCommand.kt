package com.neki.photo.application.command

import com.neki.common.domain.vo.SortOrder
import com.neki.photo.domain.enums.UploadMethod
import java.time.LocalDateTime

/**
 * fileName       : PhotoImageCommand
 * author         : koo
 * date           : 2026. 1. 2. 오후 8:28
 * description    : Photo image domain command
 */
data class UploadPhotoCommand(
    val userId: Long,
    val folderId: Long?,
    val uploads: List<UploadItem>,
    val favorite: Boolean,
) {
    data class UploadItem(
        val mediaId: Long,
        val uploadMethod: UploadMethod,
        val memo: String?,
        val capturedAt: LocalDateTime?,
    )
}

data class GetPhotosCommand(
    val userId: Long,
    val folderId: Long?,
    val page: Int,
    val size: Int,
    val sortOrder: SortOrder = SortOrder.DESC,
)

data class GetPhotoCommand(val userId: Long, val photoId: Long)

data class GetFavoritePhotosCommand(
    val userId: Long,
    val page: Int,
    val size: Int,
    val sortOrder: SortOrder = SortOrder.DESC,
)

data class DeletePhotosCommand(val userId: Long, val photoIds: List<Long>)

@Deprecated(message = "PUT API 변경 후 제거")
data class UpdatePhotoCommand(val userId: Long, val photoId: Long, val memo: String?)

data class PutPhotoCommand(val userId: Long, val photoId: Long, val memo: String?, val capturedAt: LocalDateTime?)

data class UpdatePhotoFavoriteCommand(val userId: Long, val photoId: Long, val favorite: Boolean)

data class GetFavoriteSummaryCommand(val userId: Long)
