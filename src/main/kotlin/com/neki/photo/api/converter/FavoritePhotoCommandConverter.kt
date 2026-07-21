package com.neki.photo.api.converter

import com.neki.common.domain.vo.SortOrder
import com.neki.photo.api.dto.UpdatePhotoFavoriteRequest
import com.neki.photo.application.dto.PhotoImageCommand
import com.neki.photo.application.dto.PhotoImageQuery
import org.springframework.stereotype.Component

/**
 * fileName       : FavoritePhotoCommandConverter
 * author         : koo
 * date           : 2026. 1. 14. 오전 2:23
 * description    :
 */
@Component
class FavoritePhotoCommandConverter {

    fun toGetFavoritePhotosQuery(
        userId: Long,
        page: Int,
        size: Int,
        sortOrder: SortOrder,
    ): PhotoImageQuery.GetFavoritePhotos = PhotoImageQuery.GetFavoritePhotos(
        userId = userId,
        page = page,
        size = size,
        sortOrder = sortOrder,
    )

    fun toUpdatePhotoFavoriteCommand(userId: Long, photoId: Long, request: UpdatePhotoFavoriteRequest) =
        PhotoImageCommand.UpdatePhotoFavorite(
            userId = userId,
            photoId = photoId,
            favorite = request.favorite!!,
        )

    fun toGetFavoriteSummaryQuery(userId: Long) = PhotoImageQuery.GetFavoriteSummary(userId = userId)
}
