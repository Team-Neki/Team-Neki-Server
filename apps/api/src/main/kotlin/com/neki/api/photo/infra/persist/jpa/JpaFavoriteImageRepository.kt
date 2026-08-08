package com.neki.api.photo.infra.persist.jpa

import com.neki.domain.photo.models.FavoritePhoto
import com.neki.domain.photo.models.FavoritePhotoId
import org.springframework.data.jpa.repository.JpaRepository

/**
 * fileName       : JpaFavoriteImageRepository
 * author         : koo
 * date           : 2026. 1. 13. 오후 9:28
 * description    :
 */
interface JpaFavoriteImageRepository : JpaRepository<FavoritePhoto, FavoritePhotoId> {

    fun findAllByIdUserId(userId: Long): List<FavoritePhoto>

    fun countByIdUserId(userId: Long): Long
}
