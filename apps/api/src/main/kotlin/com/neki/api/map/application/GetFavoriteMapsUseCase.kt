package com.neki.api.map.application

import com.neki.api.map.application.dto.MapResult
import com.neki.core.annotation.UseCase
import com.neki.domain.map.dto.MapQuery
import com.neki.domain.map.models.PhotoBoothLocationView
import com.neki.domain.map.service.MapService
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
