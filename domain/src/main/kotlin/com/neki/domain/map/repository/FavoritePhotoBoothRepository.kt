package com.neki.domain.map.repository

import com.neki.domain.map.models.FavoritePhotoBooth
import com.neki.domain.map.models.FavoritedPhotoBooth
import com.neki.domain.map.models.PhotoBoothKey

/**
 * fileName       : FavoritePhotoBoothRepository
 * author         : darren
 * date           : 2026. 10. 13.
 * description    : 포토부스 키 기준 즐겨찾기 (TB_FAVORITE_PHOTO_BOOTH)
 */
interface FavoritePhotoBoothRepository {

    fun add(favoritePhotoBooth: FavoritePhotoBooth)

    fun delete(userId: Long, key: PhotoBoothKey)

    fun findFavoritesByUserId(userId: Long): List<FavoritedPhotoBooth>
}
