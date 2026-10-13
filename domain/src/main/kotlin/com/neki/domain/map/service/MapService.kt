package com.neki.domain.map.service

import com.neki.core.code.ResultCode
import com.neki.core.exception.BusinessException
import com.neki.domain.map.dto.MapCommand
import com.neki.domain.map.dto.MapQuery
import com.neki.domain.map.models.FavoriteMap
import com.neki.domain.map.models.FavoritePhotoBooth
import com.neki.domain.map.models.FavoritedLocation
import com.neki.domain.map.models.FavoritedPhotoBooth
import com.neki.domain.map.models.PhotoBoothKey
import com.neki.domain.map.models.PhotoBoothLocation
import com.neki.domain.map.models.PhotoBoothLocationView
import com.neki.domain.map.models.PhotoBoothView
import com.neki.domain.map.repository.FavoriteMapRepository
import com.neki.domain.map.repository.FavoritePhotoBoothRepository
import com.neki.domain.map.repository.PhotoBoothLocationRepository
import com.neki.domain.map.repository.PhotoBoothRepository
import org.springframework.stereotype.Component

/**
 * fileName       : MapService
 * author         : koo
 * date           : 2026. 8. 3. 오전 12:31
 * description    : 포토부스 즐겨찾기.
 *   즐겨찾기는 두 테이블에 나뉘어 있다. 기존 TB_FAVORITE_MAP(지점 마스터 id) 은 옮기지 않고 남겨 두고, 새로 하는 즐겨찾기는
 *   TB_FAVORITE_PHOTO_BOOTH(포토부스 키) 에 쌓는다. 조회는 두 테이블을 합치고, 해제는 두 테이블에서 함께 지워
 *   지도 V1(지점 마스터 id) 과 V2(포토부스 키) 어느 쪽에서 바꿔도 양쪽에 같게 보인다.
 *   지점 마스터 위치와 포토부스는 원천 키 (source_platform, source_idx) = (platform, idx) 로 짝짓는다.
 */
@Component
class MapService(
    private val favoriteMapRepository: FavoriteMapRepository,
    private val favoritePhotoBoothRepository: FavoritePhotoBoothRepository,
    private val photoBoothLocationRepository: PhotoBoothLocationRepository,
    private val photoBoothRepository: PhotoBoothRepository,
) {

    // ── 지도 V1 (지점 마스터 id) ───────────────────────────────────────────────

    /** 즐겨찾기한 지점 마스터 위치. 최근 즐겨찾기한 순 */
    fun getFavoriteLocations(query: MapQuery.GetFavoriteMaps): List<PhotoBoothLocationView> {
        val favorites: List<FavoritedLocation> =
            favoriteMapRepository.findFavoriteLocationsByUserId(query.userId) +
                favoritePhotoBoothRepository.findFavoriteLocationsByUserId(query.userId)

        return favorites
            .groupBy { it.location.id }
            .values
            .map { sameLocation -> sameLocation.maxBy { it.favoritedAt } }
            .sortedByDescending { it.favoritedAt }
            .map { it.location }
    }

    /** locationIds 중 사용자가 즐겨찾기한 지점 마스터 위치 id */
    fun getFavoritedLocationIds(userId: Long, locationIds: List<Long>): Set<Long> {
        if (locationIds.isEmpty()) return emptySet()

        return favoriteMapRepository.findFavoritedLocationIds(userId, locationIds) +
            favoritePhotoBoothRepository.findFavoritedLocationIds(userId, locationIds)
    }

    /**
     * 원천 키가 있는 위치는 포토부스 키로 TB_FAVORITE_PHOTO_BOOTH 에 쌓는다.
     * 원천 키가 없는 위치(어드민 직접 입력)는 키로 가리킬 수 없어 기존처럼 TB_FAVORITE_MAP 에 쌓는다.
     */
    fun updateFavoriteMap(command: MapCommand.UpdateMapFavorite) {
        val location: PhotoBoothLocation = photoBoothLocationRepository.findById(command.locationId)
            ?: throw BusinessException(ResultCode.NOT_FOUND)

        val key: PhotoBoothKey? = location.source()
        val legacyFavorite = FavoriteMap(command.userId, command.locationId)

        if (command.favorite) {
            if (key != null) {
                favoritePhotoBoothRepository.add(FavoritePhotoBooth(command.userId, key))
            } else {
                favoriteMapRepository.add(legacyFavorite)
            }
        } else {
            favoriteMapRepository.delete(legacyFavorite)
            key?.let { favoritePhotoBoothRepository.delete(command.userId, it) }
        }
    }

    // ── 지도 V2 (포토부스 키) ─────────────────────────────────────────────────

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
            favoriteMapRepository.deleteByPhotoBoothKey(command.userId, command.key)
        }
    }

    /** 두 테이블의 즐겨찾기를 키로 합친다. 같은 부스를 양쪽에서 즐겨찾기했으면 최근 시각을 쓴다. 최근 즐겨찾기한 순 */
    private fun findFavoritedPhotoBooths(userId: Long): List<FavoritedPhotoBooth> {
        val favorites: List<FavoritedPhotoBooth> =
            favoritePhotoBoothRepository.findFavoritesByUserId(userId) +
                favoriteMapRepository.findFavoritedPhotoBooths(userId)

        return favorites
            .groupBy { it.key }
            .values
            .map { sameBooth -> sameBooth.maxBy { it.favoritedAt } }
            .sortedByDescending { it.favoritedAt }
    }
}
