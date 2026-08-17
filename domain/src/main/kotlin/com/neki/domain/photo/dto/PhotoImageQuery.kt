package com.neki.domain.photo.dto

import com.neki.core.domain.vo.Pagination

/**
 * fileName       : PhotoImageQuery
 * author         : koo
 * date           : 2026. 7. 21.
 * description    : Photo image domain query
 */
object PhotoImageQuery {
    data class GetPhotos(override val userId: Long, val folderId: Long?, val pagination: Pagination) : UserScoped

    data class GetPhoto(override val userId: Long, val photoId: Long) : UserScoped

    data class GetFavoritePhotos(override val userId: Long, val pagination: Pagination) : UserScoped

    data class GetFavoriteSummary(override val userId: Long) : UserScoped
}
