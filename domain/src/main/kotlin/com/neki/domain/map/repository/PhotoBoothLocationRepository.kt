package com.neki.domain.map.repository

import com.neki.core.domain.vo.Pagination
import com.neki.domain.map.dto.MapQuery
import com.neki.domain.map.models.PhotoBoothLocation
import com.neki.domain.map.models.PhotoBoothLocationView
import com.neki.domain.map.models.PhotoBoothLocationWithDistance
import com.neki.domain.map.models.PhotoBoothSource
import org.locationtech.jts.geom.Coordinate

/**
 * fileName       : PhotoBoothLocationRepositoryPort
 * author         : darren
 * date           : 2026. 1. 16. 11:20
 * description    :
 */
interface PhotoBoothLocationRepository {

    fun saveAll(photoBoothLocations: Collection<PhotoBoothLocation>): Collection<PhotoBoothLocation>

    fun deleteAll(photoBoothLocations: Collection<PhotoBoothLocation>)

    fun getPhotoBoothLocations(brandId: Long): List<PhotoBoothLocation>

    fun existsById(locationId: Long): Boolean

    fun listPolygonLocations(coordinates: List<Coordinate>, brandIds: List<Long>?): List<PhotoBoothLocationView>

    /**
     * [MapQuery.SearchPhotoBooths] 조건의 포토부스. 대소문자는 구분하지 않고, 지점명이 비었거나 브랜드명과 같은 부스는 뺀다.
     * 다음 페이지 판단을 위해 [Pagination.limit] 만큼 조회한다.
     */
    fun findByKeyword(query: MapQuery.SearchPhotoBooths): List<PhotoBoothLocationView>

    fun countByKeyword(query: MapQuery.SearchPhotoBooths): Long

    fun listPointLocations(
        coordinate: Coordinate,
        radiusInMeters: Int,
        brandIds: List<Long>?,
    ): List<PhotoBoothLocationWithDistance>

    /** 원천 키가 sources 에 있고 관리자가 숨기지 않은 지점 */
    fun findVisibleBySources(sources: Collection<PhotoBoothSource>): List<PhotoBoothLocation>
}
