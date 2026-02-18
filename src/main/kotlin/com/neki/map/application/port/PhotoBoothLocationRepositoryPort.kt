package com.neki.map.application.port

import com.neki.map.application.contract.PhotoBoothLocationDto
import com.neki.map.application.contract.PhotoBoothLocationWithDistanceDto
import com.neki.map.domain.entity.PhotoBoothLocation
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

    fun listPolygonLocations(coordinates: List<Coordinate>, brandIds: List<Long>?): List<PhotoBoothLocationDto>

    fun listPointLocations(
        coordinate: Coordinate,
        radiusInMeters: Int,
        brandIds: List<Long>?,
    ): List<PhotoBoothLocationWithDistanceDto>
}
