package com.neki.domain.map.repository

import com.neki.domain.map.models.FavoritePhotoBooth
import com.neki.domain.map.models.FavoritedLocation
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

    /** 지도 V1 용. 원천 키가 같은 지점 마스터 위치로 바꿔 돌려준다. 지점 마스터에 없는 키는 빠진다 */
    fun findFavoriteLocationsByUserId(userId: Long): List<FavoritedLocation>

    /** 지도 V1 용. locationIds 중 원천 키로 즐겨찾기한 지점 마스터 위치 id */
    fun findFavoritedLocationIds(userId: Long, locationIds: List<Long>): Set<Long>
}
