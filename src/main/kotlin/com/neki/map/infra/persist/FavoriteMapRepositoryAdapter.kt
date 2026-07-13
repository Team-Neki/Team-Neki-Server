package com.neki.map.infra.persist

import com.neki.map.application.contract.PhotoBoothLocationDto
import com.neki.map.application.port.FavoriteMapRepositoryPort
import com.neki.map.domain.entity.FavoriteMap
import com.neki.map.domain.entity.FavoriteMapId
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
        if (!jpaRepository.existsById(favoriteMap.id)) {
            jpaRepository.save(favoriteMap)
        }
    }

    override fun delete(favoriteMap: FavoriteMap) = jpaRepository.deleteById(favoriteMap.id)

    override fun exists(userId: Long, locationId: Long): Boolean = jpaRepository.existsById(
        FavoriteMapId(userId, locationId),
    )

    override fun findFavoriteLocationsByUserId(userId: Long): List<PhotoBoothLocationDto> =
        queryRepository.findFavoriteLocationsByUserId(userId)

    override fun findLocationIdsByUserId(userId: Long): Set<Long> =
        queryRepository.findLocationIdsByUserId(userId).toSet()
}
