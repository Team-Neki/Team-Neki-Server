package com.yapp2app.map.infra.persist

import com.yapp2app.map.application.port.PhotoBoothLocationRepositoryPort
import com.yapp2app.map.domain.entity.PhotoBoothLocation
import com.yapp2app.map.infra.persist.jpa.JpaPhotoBoothLocationRepository
import org.springframework.stereotype.Repository

/**
 * fileName       : PhotoBoothLocationAdapter
 * author         : darren
 * date           : 2026. 1. 16. 11:25
 * description    : PhotoBoothLocation 영속성에 대한 Adapter (command + query)
 */
@Repository
class PhotoBoothLocationAdapter(private val jpaRepository: JpaPhotoBoothLocationRepository) :
    PhotoBoothLocationRepositoryPort {

    override fun saveAll(photoBoothLocations: Collection<PhotoBoothLocation>): Collection<PhotoBoothLocation> =
        jpaRepository.saveAll(photoBoothLocations)

    override fun deleteAll(photoBoothLocations: Collection<PhotoBoothLocation>) =
        jpaRepository.deleteAll(photoBoothLocations)

    override fun getPhotoBoothLocations(brandId: Long): List<PhotoBoothLocation> =
        jpaRepository.findAllByBrandId(brandId)
}
