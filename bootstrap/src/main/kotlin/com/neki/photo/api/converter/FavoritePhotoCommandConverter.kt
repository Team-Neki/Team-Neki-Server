package com.neki.photo.api.converter

import com.neki.common.domain.vo.SortOrder
import com.neki.photo.api.dto.UpdatePhotoFavoriteRequest
import com.neki.photo.application.command.GetFavoritePhotosCommand
import com.neki.photo.application.command.GetFavoriteSummaryCommand
import com.neki.photo.application.command.UpdatePhotoFavoriteCommand
import org.springframework.stereotype.Component

/**
 * fileName       : FavoritePhotoCommandConverter
 * author         : koo
 * date           : 2026. 1. 14. 오전 2:23
 * description    :
 */
@Component
class FavoritePhotoCommandConverter {

    fun toGetFavoritePhotosCommand(
        userId: Long,
        page: Int,
        size: Int,
        sortOrder: SortOrder,
    ): GetFavoritePhotosCommand = GetFavoritePhotosCommand(
        userId = userId,
        page = page,
        size = size,
        sortOrder = sortOrder,
    )

    fun toUpdatePhotoFavoriteCommand(userId: Long, photoId: Long, request: UpdatePhotoFavoriteRequest) =
        UpdatePhotoFavoriteCommand(
            userId = userId,
            photoId = photoId,
            favorite = request.favorite!!,
        )

    fun toGetFavoriteSummaryCommand(userId: Long) = GetFavoriteSummaryCommand(userId = userId)
}
