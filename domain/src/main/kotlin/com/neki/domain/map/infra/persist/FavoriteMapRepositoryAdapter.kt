package com.neki.domain.map.infra.persist

import com.neki.domain.map.infra.persist.jpa.FavoriteMapQueryRepository
import com.neki.domain.map.infra.persist.jpa.JpaFavoriteMapRepository
import com.neki.domain.map.models.FavoriteMap
import com.neki.domain.map.models.FavoritedLocation
import com.neki.domain.map.models.FavoritedPhotoBooth
import com.neki.domain.map.models.PhotoBoothKey
import com.neki.domain.map.repository.FavoriteMapRepository
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
) : FavoriteMapRepository {

    override fun add(favoriteMap: FavoriteMap) {
        jpaRepository.save(favoriteMap)
    }

    override fun delete(favoriteMap: FavoriteMap) = jpaRepository.deleteById(favoriteMap.id)

    override fun exists(favoriteMap: FavoriteMap): Boolean = jpaRepository.existsById(favoriteMap.id)

    override fun deleteByPhotoBoothKey(userId: Long, key: PhotoBoothKey) =
        queryRepository.deleteByPhotoBoothKey(userId, key)

    override fun findFavoriteLocationsByUserId(userId: Long): List<FavoritedLocation> =
        queryRepository.findFavoriteLocationsByUserId(userId)

    override fun findFavoritedLocationIds(userId: Long, locationIds: List<Long>): Set<Long> =
        queryRepository.findFavoritedLocationIds(userId, locationIds).toSet()

    override fun findFavoritedPhotoBooths(userId: Long): List<FavoritedPhotoBooth> =
        queryRepository.findFavoritedPhotoBooths(userId)
}
