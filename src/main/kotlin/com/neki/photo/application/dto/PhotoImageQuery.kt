package com.neki.photo.application.dto

import com.neki.common.domain.vo.SortOrder

/**
 * fileName       : PhotoImageQuery
 * author         : koo
 * date           : 2026. 7. 21.
 * description    : Photo image domain query
 */
object PhotoImageQuery {
    data class GetPhotos(
        val userId: Long,
        val folderId: Long?,
        val page: Int,
        val size: Int,
        val sortOrder: SortOrder = SortOrder.DESC,
    )

    data class GetPhoto(val userId: Long, val photoId: Long)

    data class GetFavoritePhotos(
        val userId: Long,
        val page: Int,
        val size: Int,
        val sortOrder: SortOrder = SortOrder.DESC,
    )

    data class GetFavoriteSummary(val userId: Long)
}
