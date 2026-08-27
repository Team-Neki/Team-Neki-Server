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
import org.locationtech.jts.geom.Coordinate
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
        polygonLocations(query.coordinates)
            .filterByBrandIds(query.brandIds)
            .toList()

    fun getPointLocations(query: MapQuery.GetPointLocation): List<PhotoBoothLocationWithDistance> =
        photoBoothLocationRepository.listPointLocations(
            coordinate = query.coordinate,
            radiusInMeters = query.radiusInMeters,
            brandIds = query.brandIds,
        )

    /**
     * 다각형 영역 안의 포토부스 개수를 브랜드별로 집계한다. key 는 영역 내에 포토부스가 존재하는 브랜드 ID 다.
     */
    fun getBrandBoothCountsInPolygon(query: MapQuery.PolygonFilter): Map<Long, Long> =
        polygonLocations(query.coordinates)
            .filterByBrandIds(query.brandIds)
            .countByBrandId()

    /**
     * 폴리곤 조회는 조건이 영역뿐이라 지도 조회와 필터 조회가 동일한 쿼리를 공유한다.
     * 필터 적용과 집계는 PhotoBoothLocations 가 메모리에서 처리한다.
     */
    private fun polygonLocations(coordinates: List<Coordinate>): PhotoBoothLocations =
        PhotoBoothLocations(photoBoothLocationRepository.listPolygonLocations(coordinates))

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
