package com.neki.api.photo.api.dto

import com.neki.core.domain.vo.Pagination
import com.neki.core.domain.vo.SortOrder
import com.neki.domain.photo.dto.PhotoImageCommand
import com.neki.domain.photo.dto.PhotoImageQuery
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
            pagination = Pagination(page = page, size = size, sortOrder = sortOrder),
        )

        fun toUpdatePhotoFavoriteCommand(userId: Long, photoId: Long, request: PhotoImageRequest.UpdatePhotoFavorite) =
            PhotoImageCommand.UpdatePhotoFavorite(
                userId = userId,
                photoId = photoId,
                favorite = request.favorite!!,
            )

        fun toGetFavoriteSummaryQuery(userId: Long) = PhotoImageQuery.GetFavoriteSummary(userId = userId)
    }
}
