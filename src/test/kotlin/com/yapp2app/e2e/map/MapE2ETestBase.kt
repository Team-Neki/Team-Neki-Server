package com.yapp2app.e2e.map

import com.yapp2app.e2e.E2ETestBase
import com.yapp2app.map.domain.entity.Brand
import com.yapp2app.map.domain.entity.PhotoBoothLocation
import com.yapp2app.map.infra.persist.jpa.JpaBrandRepository
import com.yapp2app.map.infra.persist.jpa.JpaPhotoBoothLocationRepository
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

    protected val geometryFactory = GeometryFactory(PrecisionModel(), 4326)

    @AfterEach
    override fun tearDown() {
        photoBoothLocationRepository.deleteAllInBatch()
        brandRepository.deleteAllInBatch()
        super.tearDown()
    }

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
            name = name,
            address = address,
            location = geometryFactory.createPoint(Coordinate(longitude, latitude)),
        ),
    )
}