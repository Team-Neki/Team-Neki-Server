package com.neki.map.api.converter

import com.neki.map.api.dto.UpdateMapFavoriteRequest
import com.neki.map.application.dto.MapCommand
import com.neki.map.application.dto.MapQuery
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
        MapCommand.UpdateMapFavorite(
            userId = userId,
            locationId = locationId,
            favorite = request.favorite!!,
        )

    fun toGetFavoriteMapsQuery(userId: Long) = MapQuery.GetFavoriteMaps(userId = userId)
}
