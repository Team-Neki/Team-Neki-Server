package com.neki.domain.map.repository

import com.neki.domain.map.models.FavoriteMap
import com.neki.domain.map.models.FavoritedLocation
import com.neki.domain.map.models.FavoritedPhotoBooth
import com.neki.domain.map.models.PhotoBoothKey

/**
 * fileName       : FavoriteMapRepositoryPort
 * author         : darren
 * date           : 2026. 6. 21.
 * description    : 지점 마스터 id 기준 기존 즐겨찾기 (TB_FAVORITE_MAP). 새 즐겨찾기는 TB_FAVORITE_PHOTO_BOOTH 에 쌓고 여기는 읽기와 해제만 한다.
 *   단, 원천 키가 없는 지점(어드민 직접 입력)은 키로 가리킬 수 없어 V1 즐겨찾기를 계속 여기에 쌓는다
 */
interface FavoriteMapRepository {

    fun add(favoriteMap: FavoriteMap)

    fun delete(favoriteMap: FavoriteMap)

    fun exists(favoriteMap: FavoriteMap): Boolean

    /** 원천 키가 key 인 지점 마스터 위치의 즐겨찾기를 지운다 */
    fun deleteByPhotoBoothKey(userId: Long, key: PhotoBoothKey)

    fun findFavoriteLocationsByUserId(userId: Long): List<FavoritedLocation>

    fun findFavoritedLocationIds(userId: Long, locationIds: List<Long>): Set<Long>

    /** 지도 V2 용. 원천 키가 있는 지점 마스터 위치의 즐겨찾기를 키로 바꿔 돌려준다 */
    fun findFavoritedPhotoBooths(userId: Long): List<FavoritedPhotoBooth>
}
