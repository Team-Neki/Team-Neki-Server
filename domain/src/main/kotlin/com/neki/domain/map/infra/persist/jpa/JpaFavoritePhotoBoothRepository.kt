package com.neki.domain.map.infra.persist.jpa

import com.neki.domain.map.models.FavoritePhotoBooth
import com.neki.domain.map.models.FavoritePhotoBoothId
import org.springframework.data.jpa.repository.JpaRepository

/**
 * fileName       : JpaFavoritePhotoBoothRepository
 * author         : darren
 * date           : 2026. 10. 13.
 * description    : FavoritePhotoBooth JPA Repository
 */
interface JpaFavoritePhotoBoothRepository : JpaRepository<FavoritePhotoBooth, FavoritePhotoBoothId> {

    fun findAllByIdUserId(userId: Long): List<FavoritePhotoBooth>
}
