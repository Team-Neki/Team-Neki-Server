package com.neki.photo.api.dto

import com.neki.common.domain.vo.SortOrder
import com.neki.photo.application.dto.PhotoImageCommand
import com.neki.photo.application.dto.PhotoImageQuery
import org.springframework.stereotype.Component

/**
 * fileName       : FavoritePhotoConverter
 * author         : koo
 * date           : 2026. 7. 21.
 * description    : FavoritePhoto api layer converter
 */
object FavoritePhotoConverter {
    @Component
    class RequestConverter {
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
}
