package com.neki.map.api.converter

import com.neki.map.api.dto.UpdateMapFavoriteRequest
import com.neki.map.application.command.GetFavoriteMapsCommand
import com.neki.map.application.command.UpdateMapFavoriteCommand
import org.springframework.stereotype.Component

/**
 * fileName       : FavoriteMapCommandConverter
 * author         : darren
 * date           : 2026. 6. 21.
 * description    :
 */
@Component
class FavoriteMapCommandConverter {

    fun toUpdateMapFavoriteCommand(userId: Long, locationId: Long, request: UpdateMapFavoriteRequest) =
        UpdateMapFavoriteCommand(
            userId = userId,
            locationId = locationId,
            favorite = request.favorite!!,
        )

    fun toGetFavoriteMapsCommand(userId: Long) = GetFavoriteMapsCommand(userId = userId)
}
