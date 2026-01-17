package com.yapp2app.map.application.port

import com.yapp2app.map.domain.entity.PhotoBoothLocation
import com.yapp2app.map.infra.persist.jpa.PhotoBoothLocationDto
import com.yapp2app.map.infra.persist.jpa.PhotoBoothLocationWithDistanceDto

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
        coordinates: List<Pair<Double, Double>>,
        brandId: Long?,
        offset: Int,
        limit: Int,
    ): List<PhotoBoothLocationDto>

    fun listPointLocations(
        longitude: Double,
        latitude: Double,
        radiusInMeters: Int,
        brandId: Long?,
        offset: Int,
        limit: Int,
    ): List<PhotoBoothLocationWithDistanceDto>
}
