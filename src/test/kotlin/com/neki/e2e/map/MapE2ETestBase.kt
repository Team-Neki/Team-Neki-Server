package com.neki.e2e.map

import com.neki.e2e.E2ETestBase
import com.neki.map.domain.entity.Brand
import com.neki.map.domain.entity.FavoriteMap
import com.neki.map.domain.entity.PhotoBoothLocation
import com.neki.map.infra.persist.jpa.JpaBrandRepository
import com.neki.map.infra.persist.jpa.JpaFavoriteMapRepository
import com.neki.map.infra.persist.jpa.JpaPhotoBoothLocationRepository
import com.neki.map.infra.persist.jpa.JpaUserBrandOrderRepository
import org.junit.jupiter.api.AfterEach
import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.GeometryFactory
import org.locationtech.jts.geom.PrecisionModel
import org.springframework.beans.factory.annotation.Autowired

/**
 * fileName       : MapE2ETestBase
 * author         : darren
 * date           : 2026. 1. 21. 17:03
 * description    : Map E2E 테스트를 위한 Base Class
 */
abstract class MapE2ETestBase : E2ETestBase() {

    @Autowired
    protected lateinit var brandRepository: JpaBrandRepository

    @Autowired
    protected lateinit var photoBoothLocationRepository: JpaPhotoBoothLocationRepository

    @Autowired
    protected lateinit var favoriteMapRepository: JpaFavoriteMapRepository

    @Autowired
    protected lateinit var userBrandOrderRepository: JpaUserBrandOrderRepository

    protected val geometryFactory = GeometryFactory(PrecisionModel(), 4326)

    @AfterEach
    override fun tearDown() {
        userBrandOrderRepository.deleteAllInBatch()
        favoriteMapRepository.deleteAllInBatch()
        photoBoothLocationRepository.deleteAllInBatch()
        brandRepository.deleteAllInBatch()
        super.tearDown()
    }

    protected fun favoriteMap(userId: Long, locationId: Long): FavoriteMap = favoriteMapRepository.save(
        FavoriteMap(userId = userId, locationId = locationId),
    )

    protected fun createBrand(name: String, code: String): Brand = brandRepository.save(
        Brand(name = name, code = code),
    )

    protected fun createPhotoBoothLocation(
        brandId: Long,
        name: String,
        address: String,
        longitude: Double,
        latitude: Double,
    ): PhotoBoothLocation = photoBoothLocationRepository.save(
        PhotoBoothLocation(
            mapId = "test-map-id-${System.currentTimeMillis()}",
            brandId = brandId,
            branchName = name,
            address = address,
            location = geometryFactory.createPoint(Coordinate(longitude, latitude)),
        ),
    )
}
