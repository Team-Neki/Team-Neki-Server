package com.neki.api.e2e.map

import com.neki.api.e2e.E2ETestBase
import com.neki.domain.map.infra.persist.jpa.JpaBrandRepository
import com.neki.domain.map.infra.persist.jpa.JpaFavoriteMapRepository
import com.neki.domain.map.infra.persist.jpa.JpaFavoritePhotoBoothRepository
import com.neki.domain.map.infra.persist.jpa.JpaPhotoBoothLocationRepository
import com.neki.domain.map.infra.persist.jpa.JpaUserBrandOrderRepository
import com.neki.domain.map.models.Brand
import com.neki.domain.map.models.FavoriteMap
import com.neki.domain.map.models.FavoritePhotoBooth
import com.neki.domain.map.models.PhotoBoothKey
import com.neki.domain.map.models.PhotoBoothLocation
import com.neki.domain.search.infra.persist.jpa.JpaPhotoBoothEnrichedRepository
import com.neki.domain.search.models.PhotoBoothEnriched
import com.neki.domain.search.models.PhotoBoothEnrichedId
import org.junit.jupiter.api.AfterEach
import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.GeometryFactory
import org.locationtech.jts.geom.PrecisionModel
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDate

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

    @Autowired
    protected lateinit var favoritePhotoBoothRepository: JpaFavoritePhotoBoothRepository

    @Autowired
    protected lateinit var photoBoothEnrichedRepository: JpaPhotoBoothEnrichedRepository

    protected val geometryFactory = GeometryFactory(PrecisionModel(), 4326)

    @AfterEach
    override fun tearDown() {
        userBrandOrderRepository.deleteAllInBatch()
        favoriteMapRepository.deleteAllInBatch()
        favoritePhotoBoothRepository.deleteAllInBatch()
        photoBoothEnrichedRepository.deleteAllInBatch()
        photoBoothLocationRepository.deleteAllInBatch()
        brandRepository.deleteAllInBatch()
        super.tearDown()
    }

    protected fun favoriteMap(userId: Long, locationId: Long): FavoriteMap = favoriteMapRepository.save(
        FavoriteMap(userId = userId, locationId = locationId),
    )

    protected fun createBrand(name: String, code: String, exposeToMap: Boolean = true): Brand = brandRepository.save(
        Brand(name = name, code = code, exposeToMap = exposeToMap),
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

    protected fun favoritePhotoBooth(userId: Long, key: PhotoBoothKey): FavoritePhotoBooth =
        favoritePhotoBoothRepository.save(FavoritePhotoBooth(userId = userId, key = key))

    /**
     * Workflow 가 적재한 것과 같은 지금 세대 포토부스(tb_photo_booth_enriched). platform 은 브랜드 코드다
     */
    protected fun createEnrichedPhotoBooth(
        brand: Brand,
        idx: String,
        name: String,
        longitude: Double = 127.027456,
        latitude: Double = 37.497946,
        address: String? = "서울 강남구 $name",
    ): PhotoBoothKey {
        photoBoothEnrichedRepository.save(
            PhotoBoothEnriched(
                id = PhotoBoothEnrichedId(platform = brand.code, idx = idx),
                name = name,
                address = address,
                location = geometryFactory.createPoint(Coordinate(longitude, latitude)),
                sourceDt = LocalDate.of(2026, 10, 12),
                bCode = null,
            ),
        )
        return PhotoBoothKey(platform = brand.code, idx = idx)
    }

    /**
     * stores-sync 가 만든 것과 같은 지점 마스터 위치. 원천 키가 key 와 같다
     */
    protected fun createSourcedPhotoBoothLocation(brand: Brand, key: PhotoBoothKey, name: String): PhotoBoothLocation =
        photoBoothLocationRepository.save(
            PhotoBoothLocation(
                mapId = key.toBoothId(),
                brandId = brand.id!!,
                branchName = name,
                address = "서울 강남구 $name",
                location = geometryFactory.createPoint(Coordinate(127.027456, 37.497946)),
                sourcePlatform = key.platform,
                sourceIdx = key.idx,
            ),
        )
}
