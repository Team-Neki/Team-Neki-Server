package com.neki.domain.map.infra.persist

import com.neki.domain.map.infra.persist.jpa.JpaPhotoBoothLocationRepository
import com.neki.domain.map.models.PhotoBoothLocation
import com.neki.domain.map.repository.PhotoBoothLocationRepository
import org.springframework.stereotype.Repository

/**
 * fileName       : PhotoBoothLocationAdapter
 * author         : darren
 * date           : 2026. 1. 16. 11:25
 * description    : PhotoBoothLocation 영속성에 대한 Adapter (query + query)
 */
@Repository
class PhotoBoothLocationRepositoryAdapter(private val jpaRepository: JpaPhotoBoothLocationRepository) :
    PhotoBoothLocationRepository {

    override fun saveAll(photoBoothLocations: Collection<PhotoBoothLocation>): Collection<PhotoBoothLocation> =
        jpaRepository.saveAll(photoBoothLocations)

    override fun deleteAll(photoBoothLocations: Collection<PhotoBoothLocation>) =
        jpaRepository.deleteAll(photoBoothLocations)

    override fun getPhotoBoothLocations(brandId: Long): List<PhotoBoothLocation> =
        jpaRepository.findAllByBrandId(brandId)
}
