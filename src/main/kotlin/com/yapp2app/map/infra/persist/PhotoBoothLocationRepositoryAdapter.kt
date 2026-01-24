package com.yapp2app.map.infra.persist

import com.yapp2app.map.application.contract.PhotoBoothLocationDto
import com.yapp2app.map.application.contract.PhotoBoothLocationWithDistanceDto
import com.yapp2app.map.application.port.PhotoBoothLocationRepositoryPort
import com.yapp2app.map.domain.entity.PhotoBoothLocation
import com.yapp2app.map.infra.persist.jpa.JpaPhotoBoothLocationRepository
import com.yapp2app.map.infra.persist.jpa.PhotoBoothLocationQueryRepository
import org.locationtech.jts.geom.Coordinate
import org.springframework.stereotype.Repository

/**
 * fileName       : PhotoBoothLocationAdapter
 * author         : darren
 * date           : 2026. 1. 16. 11:25
 * description    : PhotoBoothLocation 영속성에 대한 Adapter (command + query)
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

    override fun listPolygonLocations(
        coordinates: List<Coordinate>,
        brandIds: List<Long>?,
        offset: Int,
        limit: Int,
    ): List<PhotoBoothLocationDto> = queryRepository.findByPolygon(coordinates, brandIds, offset, limit)

    override fun listPointLocations(
        coordinate: Coordinate,
        radiusInMeters: Int,
        brandIds: List<Long>?,
        offset: Int,
        limit: Int,
    ): List<PhotoBoothLocationWithDistanceDto> =
        queryRepository.findByDistanceFromPoint(coordinate, radiusInMeters, brandIds, offset, limit)
}
