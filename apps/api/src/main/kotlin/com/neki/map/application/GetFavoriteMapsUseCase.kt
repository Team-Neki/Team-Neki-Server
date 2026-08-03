package com.neki.map.application

import com.neki.common.annotation.UseCase
import com.neki.map.application.dto.MapResult
import com.neki.map.dto.MapQuery
import com.neki.map.models.PhotoBoothLocationView
import com.neki.map.service.MapService
import org.springframework.transaction.annotation.Transactional

/**
 * fileName       : GetFavoriteMapsUseCase
 * author         : darren
 * date           : 2026. 6. 21.
 * description    :
 */
@UseCase
class GetFavoriteMapsUseCase(private val mapService: MapService) {

    @Transactional(readOnly = true)
    fun execute(query: MapQuery.GetFavoriteMaps): MapResult.GetFavoriteMap {
        val locations: List<PhotoBoothLocationView> = mapService.getFavoriteLocations(query)

        return MapResult.GetFavoriteMap(locations = locations)
    }
}
