package com.neki.domain.map.service

import com.neki.core.code.ResultCode
import com.neki.core.exception.BusinessException
import com.neki.domain.map.dto.MapCommand
import com.neki.domain.map.dto.MapQuery
import com.neki.domain.map.models.FavoritePhotoBooth
import com.neki.domain.map.models.FavoritedPhotoBooth
import com.neki.domain.map.models.PhotoBoothKey
import com.neki.domain.map.models.PhotoBoothView
import com.neki.domain.map.repository.FavoritePhotoBoothRepository
import com.neki.domain.map.repository.PhotoBoothRepository
import org.springframework.stereotype.Component

/**
 * fileName       : MapService
 * author         : koo
 * date           : 2026. 8. 3. 오전 12:31
 * description    : 포토부스 즐겨찾기. 포토부스 키(원천 키 (platform, idx)) 기준 TB_FAVORITE_PHOTO_BOOTH 만 본다.
 *   지도 V1 제거와 함께 기존 TB_FAVORITE_MAP(지점 마스터 id) 즐겨찾기는 V47 에서 이 테이블로 옮겼다.
 */
@Component
class MapService(
    private val favoritePhotoBoothRepository: FavoritePhotoBoothRepository,
    private val photoBoothRepository: PhotoBoothRepository,
) {

    /** 즐겨찾기한 포토부스. 최근 즐겨찾기한 순이고, 지금 세대(enriched)에 없는 부스는 빠진다 */
    fun getFavoritePhotoBooths(query: MapQuery.GetFavoriteMaps): List<PhotoBoothView> {
        val favorites: List<FavoritedPhotoBooth> = findFavoritedPhotoBooths(query.userId)
        if (favorites.isEmpty()) return emptyList()

        val boothByKey: Map<PhotoBoothKey, PhotoBoothView> =
            photoBoothRepository.findByKeys(favorites.map { it.key }).associateBy { it.key }

        return favorites.mapNotNull { boothByKey[it.key] }
    }

    /** 사용자가 즐겨찾기한 모든 포토부스 키. 사용자 즐겨찾기는 많지 않아 범위를 좁히지 않고 전부 읽는다 */
    fun getFavoritedPhotoBoothKeys(userId: Long): Set<PhotoBoothKey> =
        findFavoritedPhotoBooths(userId).map { it.key }.toSet()

    /**
     * 지금 세대에 없는 부스는 즐겨찾기할 수 없다. 해제는 부스가 세대에서 빠졌어도(폐점 등) 남은 즐겨찾기를 지울 수 있게 확인하지 않는다
     */
    fun updateFavoritePhotoBooth(command: MapCommand.UpdatePhotoBoothFavorite) {
        if (command.favorite) {
            if (!photoBoothRepository.existsByKey(command.key)) throw BusinessException(ResultCode.NOT_FOUND)

            favoritePhotoBoothRepository.add(FavoritePhotoBooth(command.userId, command.key))
        } else {
            favoritePhotoBoothRepository.delete(command.userId, command.key)
        }
    }

    /** 최근 즐겨찾기한 순 */
    private fun findFavoritedPhotoBooths(userId: Long): List<FavoritedPhotoBooth> =
        favoritePhotoBoothRepository.findFavoritesByUserId(userId).sortedByDescending { it.favoritedAt }
}
