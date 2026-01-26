package com.yapp2app.photo.application.command

import com.yapp2app.common.domain.vo.SortOrder

/**
 * fileName       : PhotoImageCommand
 * author         : koo
 * date           : 2026. 1. 2. 오후 8:28
 * description    : Photo image domain command
 */
data class UploadPhotoCommand(val userId: Long, val folderId: Long?, val uploads: List<UploadItem>) {
    data class UploadItem(val mediaId: Long, val memo: String?)
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

data class DeletePhotoCommand(val userId: Long, val photoId: Long)

data class DeletePhotosCommand(val userId: Long, val photoIds: List<Long>)

data class UpdatePhotoCommand(val userId: Long, val photoId: Long, val memo: String?)

data class UpdatePhotoFavoriteCommand(val userId: Long, val photoId: Long, val favorite: Boolean)

data class GetFavoriteSummaryCommand(val userId: Long)
