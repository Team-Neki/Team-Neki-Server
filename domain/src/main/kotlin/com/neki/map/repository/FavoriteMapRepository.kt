package com.neki.map.repository

import com.neki.map.models.FavoriteMap
import com.neki.map.models.PhotoBoothLocationView

/**
 * fileName       : FavoriteMapRepositoryPort
 * author         : darren
 * date           : 2026. 6. 21.
 * description    :
 */
interface FavoriteMapRepository {

    fun add(favoriteMap: FavoriteMap)

    fun delete(favoriteMap: FavoriteMap)

    fun exists(favoriteMap: FavoriteMap): Boolean

    fun findFavoriteLocationsByUserId(userId: Long): List<PhotoBoothLocationView>

    fun findFavoritedLocationIds(userId: Long, locationIds: List<Long>): Set<Long>
}
