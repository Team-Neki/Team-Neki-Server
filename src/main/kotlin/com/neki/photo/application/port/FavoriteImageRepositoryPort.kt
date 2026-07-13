package com.neki.photo.application.port

import com.neki.photo.domain.entity.FavoritePhoto

/**
 * fileName       : FavoriteImageRepository
 * author         : koo
 * date           : 2026. 1. 13. 오후 9:29
 * description    :
 */
interface FavoriteImageRepositoryPort {

    fun add(favoritePhoto: FavoritePhoto)

    fun addAll(userId: Long, photoIds: List<Long>)

    fun delete(favoritePhoto: FavoritePhoto)

    fun deleteAll(userId: Long, photoIds: List<Long>)

    fun exists(userId: Long, photoId: Long): Boolean

    fun findPhotoIdsByUserId(userId: Long): Set<Long>

    fun countByUserId(userId: Long): Long
}
