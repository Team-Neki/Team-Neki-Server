package com.neki.map.application.usecase

import com.neki.common.annotation.UseCase
import com.neki.map.application.contract.PhotoBoothLocationDto
import com.neki.map.application.dto.MapQuery
import com.neki.map.application.dto.MapResult
import com.neki.map.application.port.FavoriteMapRepositoryPort
import org.springframework.transaction.annotation.Transactional

/**
 * fileName       : GetFavoriteMapsUseCase
 * author         : darren
 * date           : 2026. 6. 21.
 * description    :
 */
@UseCase
class GetFavoriteMapsUseCase(private val favoriteMapRepository: FavoriteMapRepositoryPort) {

    @Transactional(readOnly = true)
    fun execute(query: MapQuery.GetFavoriteMaps): MapResult.GetFavoriteMap {
        val locations: List<PhotoBoothLocationDto> =
            favoriteMapRepository.findFavoriteLocationsByUserId(query.userId)

        return MapResult.GetFavoriteMap(locations = locations)
    }
}
