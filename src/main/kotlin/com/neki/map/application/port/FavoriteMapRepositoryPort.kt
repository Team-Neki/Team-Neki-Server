package com.neki.map.application.port

import com.neki.map.application.contract.PhotoBoothLocationDto

/**
 * fileName       : FavoriteMapRepositoryPort
 * author         : darren
 * date           : 2026. 6. 21.
 * description    :
 */
interface FavoriteMapRepositoryPort {

    fun add(userId: Long, locationId: Long)

    fun delete(userId: Long, locationId: Long)

    fun exists(userId: Long, locationId: Long): Boolean

    fun findFavoriteLocationsByUserId(userId: Long): List<PhotoBoothLocationDto>

    fun findLocationIdsByUserId(userId: Long): Set<Long>
}
