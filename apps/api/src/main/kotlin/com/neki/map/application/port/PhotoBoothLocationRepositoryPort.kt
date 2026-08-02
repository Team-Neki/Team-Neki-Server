package com.neki.map.application.port

import com.neki.map.application.port.dto.MapContract
import com.neki.map.entity.PhotoBoothLocation
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

    fun existsById(locationId: Long): Boolean

    fun listPolygonLocations(coordinates: List<Coordinate>, brandIds: List<Long>?): List<MapContract.PhotoBoothLocation>

    fun listPointLocations(
        coordinate: Coordinate,
        radiusInMeters: Int,
        brandIds: List<Long>?,
    ): List<MapContract.PhotoBoothLocationWithDistance>
}
