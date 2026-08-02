package com.neki.map.infra.persist

import com.neki.map.application.port.FavoriteMapRepositoryPort
import com.neki.map.application.port.dto.MapContract
import com.neki.map.entity.FavoriteMap
import com.neki.map.infra.persist.jpa.FavoriteMapQueryRepository
import com.neki.map.infra.persist.jpa.JpaFavoriteMapRepository
import org.springframework.stereotype.Repository

/**
 * fileName       : FavoriteMapRepositoryAdapter
 * author         : darren
 * date           : 2026. 6. 21.
 * description    :
 */
@Repository
class FavoriteMapRepositoryAdapter(
    private val jpaRepository: JpaFavoriteMapRepository,
    private val queryRepository: FavoriteMapQueryRepository,
) : FavoriteMapRepositoryPort {

    override fun add(favoriteMap: FavoriteMap) {
        jpaRepository.save(favoriteMap)
    }

    override fun delete(favoriteMap: FavoriteMap) = jpaRepository.deleteById(favoriteMap.id)

    override fun exists(favoriteMap: FavoriteMap): Boolean = jpaRepository.existsById(favoriteMap.id)

    override fun findFavoriteLocationsByUserId(userId: Long): List<MapContract.PhotoBoothLocation> =
        queryRepository.findFavoriteLocationsByUserId(userId)

    override fun findFavoritedLocationIds(userId: Long, locationIds: List<Long>): Set<Long> =
        queryRepository.findFavoritedLocationIds(userId, locationIds).toSet()
}
