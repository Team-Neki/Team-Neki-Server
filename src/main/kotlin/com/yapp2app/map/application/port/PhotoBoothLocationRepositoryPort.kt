package com.yapp2app.map.application.port

import com.yapp2app.map.application.contract.PhotoBoothLocationDto
import com.yapp2app.map.application.contract.PhotoBoothLocationWithDistanceDto
import com.yapp2app.map.domain.entity.PhotoBoothLocation
import org.locationtech.jts.geom.Coordinate

/**
 * fileName       : PhotoBoothLocationRepositoryPort
 * author         : darren
 * date           : 2026. 1. 16. 11:20
 * description    :
 */
interface PhotoBoothLocationRepositoryPort {

    fun saveAll(photoBoothLocations: Collection<PhotoBoothLocation>): Collection<PhotoBoothLocation>

    fun deleteAll(photoBoothLocations: Collection<PhotoBoothLocation>)

    fun getPhotoBoothLocations(brandId: Long): List<PhotoBoothLocation>

    fun listPolygonLocations(
        coordinates: List<Coordinate>,
        brandIds: List<Long>?,
        offset: Int,
        limit: Int,
    ): List<PhotoBoothLocationDto>

    fun listPointLocations(
        coordinate: Coordinate,
        radiusInMeters: Int,
        brandIds: List<Long>?,
        offset: Int,
        limit: Int,
    ): List<PhotoBoothLocationWithDistanceDto>
}
