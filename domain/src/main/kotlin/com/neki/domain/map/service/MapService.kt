package com.neki.domain.map.service

import com.neki.core.code.ResultCode
import com.neki.core.exception.BusinessException
import com.neki.domain.map.dto.MapCommand
import com.neki.domain.map.dto.MapQuery
import com.neki.domain.map.models.FavoriteMap
import com.neki.domain.map.models.PhotoBoothLocationView
import com.neki.domain.map.repository.FavoriteMapRepository
import com.neki.domain.map.repository.PhotoBoothLocationRepository
import org.springframework.stereotype.Component

/**
 * fileName       : MapService
 * author         : koo
 * date           : 2026. 8. 3. 오전 12:31
 * description    :
 */
@Component
class MapService(
    private val favoriteMapRepository: FavoriteMapRepository,
    private val photoBoothLocationRepository: PhotoBoothLocationRepository,
) {

    fun getFavoriteLocations(query: MapQuery.GetFavoriteMaps): List<PhotoBoothLocationView> =
        favoriteMapRepository.findFavoriteLocationsByUserId(query.userId)

    fun updateFavoriteMap(command: MapCommand.UpdateMapFavorite) {
        val locationExists: Boolean = photoBoothLocationRepository.existsById(command.locationId)

        if (!locationExists) throw BusinessException(ResultCode.NOT_FOUND)

        val favoriteMap = FavoriteMap(command.userId, command.locationId)
        if (command.favorite) {
            favoriteMapRepository.add(favoriteMap)
        } else {
            favoriteMapRepository.delete(favoriteMap)
        }
    }
}
