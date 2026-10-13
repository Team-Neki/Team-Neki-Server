package com.neki.domain.map.infra.persist

import com.neki.domain.map.infra.persist.jpa.FavoritePhotoBoothQueryRepository
import com.neki.domain.map.infra.persist.jpa.JpaFavoritePhotoBoothRepository
import com.neki.domain.map.models.FavoritePhotoBooth
import com.neki.domain.map.models.FavoritePhotoBoothId
import com.neki.domain.map.models.FavoritedLocation
import com.neki.domain.map.models.FavoritedPhotoBooth
import com.neki.domain.map.models.PhotoBoothKey
import com.neki.domain.map.repository.FavoritePhotoBoothRepository
import org.springframework.stereotype.Repository

/**
 * fileName       : FavoritePhotoBoothRepositoryAdapter
 * author         : darren
 * date           : 2026. 10. 13.
 * description    :
 */
@Repository
class FavoritePhotoBoothRepositoryAdapter(
    private val jpaRepository: JpaFavoritePhotoBoothRepository,
    private val queryRepository: FavoritePhotoBoothQueryRepository,
) : FavoritePhotoBoothRepository {

    override fun add(favoritePhotoBooth: FavoritePhotoBooth) {
        jpaRepository.save(favoritePhotoBooth)
    }

    override fun delete(userId: Long, key: PhotoBoothKey) =
        jpaRepository.deleteById(FavoritePhotoBoothId(userId = userId, platform = key.platform, idx = key.idx))

    override fun findFavoritesByUserId(userId: Long): List<FavoritedPhotoBooth> =
        jpaRepository.findAllByIdUserId(userId).map { FavoritedPhotoBooth(it.id.key(), it.createdAt!!) }

    override fun findFavoriteLocationsByUserId(userId: Long): List<FavoritedLocation> =
        queryRepository.findFavoriteLocationsByUserId(userId)

    override fun findFavoritedLocationIds(userId: Long, locationIds: List<Long>): Set<Long> =
        queryRepository.findFavoritedLocationIds(userId, locationIds).toSet()
}
