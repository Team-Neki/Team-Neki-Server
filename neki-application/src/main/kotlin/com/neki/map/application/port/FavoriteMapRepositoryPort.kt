package com.neki.map.application.port

import com.neki.map.application.contract.PhotoBoothLocationDto
import com.neki.map.entity.FavoriteMap

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

    fun findFavoriteLocationsByUserId(userId: Long): List<PhotoBoothLocationDto>

    fun findFavoritedLocationIds(userId: Long, locationIds: List<Long>): Set<Long>
}
