package com.yapp2app.photo.application.port

/**
 * fileName       : FavoriteImageRepository
 * author         : koo
 * date           : 2026. 1. 13. 오후 9:29
 * description    :
 */
interface FavoriteImageRepositoryPort {

    fun add(userId: Long, photoId: Long)

    fun remove(userId: Long, photoId: Long)

    fun exists(userId: Long, photoId: Long): Boolean

    fun findPhotoIdsByUserId(userId: Long): Set<Long>

    fun countByUserId(userId: Long): Long
}
