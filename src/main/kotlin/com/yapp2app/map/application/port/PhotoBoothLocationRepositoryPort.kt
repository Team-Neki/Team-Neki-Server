package com.yapp2app.map.application.port

import com.yapp2app.map.domain.entity.PhotoBoothLocation

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
}
