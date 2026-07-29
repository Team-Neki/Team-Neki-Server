package com.neki.map.application.port

import com.neki.map.application.port.dto.MapContract
import com.neki.map.domain.entity.FavoriteMap

/**
 * fileName       : FavoriteMapRepositoryPort
 * author         : darren
 * date           : 2026. 6. 21.
 * description    :
 */
interface FavoriteMapRepositoryPort {

    fun add(favoriteMap: FavoriteMap)

    fun delete(favoriteMap: FavoriteMap)

    fun exists(favoriteMap: FavoriteMap): Boolean

    fun findFavoriteLocationsByUserId(userId: Long): List<MapContract.PhotoBoothLocation>

    fun findFavoritedLocationIds(userId: Long, locationIds: List<Long>): Set<Long>
}
