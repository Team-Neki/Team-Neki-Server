package com.yapp2app.map.infra.persist.jpa

import com.yapp2app.map.domain.entity.PhotoBoothLocation
import org.springframework.data.jpa.repository.JpaRepository

/**
 * fileName       : JpaPhotoBoothLocationRepository
 * author         : darren
 * date           : 2026. 01. 13.
 * description    : PhotoBoothLocation JPA Repository
 */
interface JpaPhotoBoothLocationRepository : JpaRepository<PhotoBoothLocation, Long> {

    fun findAllByBrandId(brandId: Long): List<PhotoBoothLocation>
}
