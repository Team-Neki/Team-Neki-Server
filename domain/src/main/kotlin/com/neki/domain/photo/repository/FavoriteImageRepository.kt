package com.neki.domain.photo.repository

import com.neki.domain.photo.models.FavoritePhoto

/**
 * fileName       : FavoriteImageRepository
 * author         : koo
 * date           : 2026. 1. 13. 오후 9:29
 * description    :
 */
interface FavoriteImageRepository {

    fun add(favoritePhoto: FavoritePhoto)

    fun addAll(userId: Long, photoIds: List<Long>)

    fun delete(favoritePhoto: FavoritePhoto)

    fun deleteAll(userId: Long, photoIds: List<Long>)

    fun exists(favoritePhoto: FavoritePhoto): Boolean

    fun findPhotoIdsByUserId(userId: Long): Set<Long>

    fun countByUserId(userId: Long): Long
}
