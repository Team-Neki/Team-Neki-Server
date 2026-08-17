package com.neki.api.map.api.dto

import com.neki.domain.map.dto.MapCommand
import com.neki.domain.map.dto.MapQuery
import org.springframework.stereotype.Component

/**
 * fileName       : FavoriteMapConverter
 * author         : koo
 * date           : 2026. 7. 21.
 * description    : FavoriteMap api layer converter
 */
object FavoriteMapConverter {
    @Component
    class RequestConverter {
        fun toUpdateMapFavoriteCommand(userId: Long, locationId: Long, request: MapRequest.UpdateMapFavorite) =
            MapCommand.UpdateMapFavorite(
                userId = userId,
                locationId = locationId,
                favorite = request.favorite!!,
            )

        fun toGetFavoriteMapsQuery(userId: Long) = MapQuery.GetFavoriteMaps(userId = userId)
    }
}
