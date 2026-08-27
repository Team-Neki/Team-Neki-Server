package com.neki.domain.map.service

import com.neki.core.code.ResultCode
import com.neki.core.exception.BusinessException
import com.neki.domain.map.dto.MapCommand
import com.neki.domain.map.dto.MapQuery
import com.neki.domain.map.models.FavoriteMap
import com.neki.domain.map.models.PhotoBoothLocation
import com.neki.domain.map.models.PhotoBoothLocationView
import com.neki.domain.map.models.PhotoBoothLocationWithDistance
import com.neki.domain.map.models.PhotoBoothLocations
import com.neki.domain.map.repository.FavoriteMapRepository
import com.neki.domain.map.repository.PhotoBoothLocationRepository
import org.springframework.stereotype.Component

/**
 * fileName       : MapService
 * author         : koo
 * date           : 2026. 8. 3. 오전 12:31
 * description    :
 */
@Component
class MapService(
    private val favoriteMapRepository: FavoriteMapRepository,
    private val photoBoothLocationRepository: PhotoBoothLocationRepository,
) {

    fun getFavoriteLocations(query: MapQuery.GetFavoriteMaps): List<PhotoBoothLocationView> =
        favoriteMapRepository.findFavoriteLocationsByUserId(query.userId)

    fun updateFavoriteMap(command: MapCommand.UpdateMapFavorite) {
        val locationExists: Boolean = photoBoothLocationRepository.existsById(command.locationId)

        if (!locationExists) throw BusinessException(ResultCode.NOT_FOUND)

        val favoriteMap = FavoriteMap(command.userId, command.locationId)
        if (command.favorite) {
            favoriteMapRepository.add(favoriteMap)
        } else {
            favoriteMapRepository.delete(favoriteMap)
        }
    }

    fun getPolygonLocations(query: MapQuery.GetPolygonLocation): List<PhotoBoothLocationView> =
        photoBoothLocationRepository.listPolygonLocations(
            coordinates = query.coordinates,
            brandIds = query.brandIds,
        )

    fun getPointLocations(query: MapQuery.GetPointLocation): List<PhotoBoothLocationWithDistance> =
        photoBoothLocationRepository.listPointLocations(
            coordinate = query.coordinate,
            radiusInMeters = query.radiusInMeters,
            brandIds = query.brandIds,
        )

    /**
     * 다각형 영역 안의 포토부스 개수를 브랜드별로 집계한다. key 는 영역 내에 포토부스가 존재하는 브랜드 ID 다.
     *
     * 영역 내 지점을 한 번만 읽어 메모리에서 집계한다. 필터 축이 늘어나도 조회는 그대로 1회이고
     * PhotoBoothLocations 에 집계 메서드만 추가하면 되므로 DB 왕복이 축 개수만큼 늘지 않는다.
     */
    fun getBrandBoothCountsInPolygon(query: MapQuery.PolygonFilter): Map<Long, Long> = PhotoBoothLocations(
        photoBoothLocationRepository.listPolygonLocations(
            coordinates = query.coordinates,
            brandIds = query.brandIds,
        ),
    ).countByBrandId()

    /**
     * locationIds 는 오케스트레이션 중 결정되는 값이므로 userId 와 함께 받는다.
     */
    fun findFavoritedLocationIds(userId: Long, locationIds: List<Long>): Set<Long> =
        favoriteMapRepository.findFavoritedLocationIds(userId, locationIds)

    fun getLocationsByBrandId(brandId: Long): List<PhotoBoothLocation> =
        photoBoothLocationRepository.getPhotoBoothLocations(brandId)

    fun saveLocations(locations: Collection<PhotoBoothLocation>): Collection<PhotoBoothLocation> =
        photoBoothLocationRepository.saveAll(locations)

    fun deleteLocations(locations: Collection<PhotoBoothLocation>) = photoBoothLocationRepository.deleteAll(locations)
}
