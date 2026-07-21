package com.neki.map.infra.persist

import com.neki.map.application.port.PhotoBoothLocationRepositoryPort
import com.neki.map.application.port.dto.MapContract
import com.neki.map.domain.entity.PhotoBoothLocation
import com.neki.map.infra.persist.jpa.JpaPhotoBoothLocationRepository
import com.neki.map.infra.persist.jpa.PhotoBoothLocationQueryRepository
import org.locationtech.jts.geom.Coordinate
import org.springframework.stereotype.Repository

/**
 * fileName       : PhotoBoothLocationAdapter
 * author         : darren
 * date           : 2026. 1. 16. 11:25
 * description    : PhotoBoothLocation 영속성에 대한 Adapter (query + query)
 */
@Repository
class PhotoBoothLocationRepositoryAdapter(
    private val jpaRepository: JpaPhotoBoothLocationRepository,
    private val queryRepository: PhotoBoothLocationQueryRepository,
) : PhotoBoothLocationRepositoryPort {

    override fun saveAll(photoBoothLocations: Collection<PhotoBoothLocation>): Collection<PhotoBoothLocation> =
        jpaRepository.saveAll(photoBoothLocations)

    override fun deleteAll(photoBoothLocations: Collection<PhotoBoothLocation>) =
        jpaRepository.deleteAll(photoBoothLocations)

    override fun getPhotoBoothLocations(brandId: Long): List<PhotoBoothLocation> =
        jpaRepository.findAllByBrandId(brandId)

    override fun existsById(locationId: Long): Boolean = jpaRepository.existsById(locationId)

    override fun listPolygonLocations(
        coordinates: List<Coordinate>,
        brandIds: List<Long>?,
    ): List<MapContract.PhotoBoothLocation> = queryRepository.findByPolygon(coordinates, brandIds)

    override fun listPointLocations(
        coordinate: Coordinate,
        radiusInMeters: Int,
        brandIds: List<Long>?,
    ): List<MapContract.PhotoBoothLocationWithDistance> =
        queryRepository.findByDistanceFromPoint(coordinate, radiusInMeters, brandIds)
}
