package com.neki.map

import com.neki.map.models.PhotoBoothLocation
import com.neki.map.models.PhotoBoothLocationView
import com.neki.map.models.PhotoBoothLocationWithDistance
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

    fun listPointLocations(
        coordinate: Coordinate,
        radiusInMeters: Int,
        brandIds: List<Long>?,
    ): List<PhotoBoothLocationWithDistance>
}
