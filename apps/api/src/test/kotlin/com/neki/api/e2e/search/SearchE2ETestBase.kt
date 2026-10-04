package com.neki.api.e2e.search

import com.neki.api.e2e.E2ETestBase
import com.neki.domain.map.infra.persist.jpa.JpaBrandRepository
import com.neki.domain.map.infra.persist.jpa.JpaFavoriteMapRepository
import com.neki.domain.map.infra.persist.jpa.JpaPhotoBoothLocationRepository
import com.neki.domain.map.infra.persist.jpa.JpaUserBrandOrderRepository
import com.neki.domain.map.models.Brand
import com.neki.domain.map.models.FavoriteMap
import com.neki.domain.map.models.PhotoBoothLocation
import com.neki.domain.map.models.UserBrandOrder
import com.neki.domain.search.SearchNormalizer
import com.neki.domain.search.infra.persist.jpa.JpaLegalDongRepository
import com.neki.domain.search.infra.persist.jpa.JpaPhotoBoothSearchRepository
import com.neki.domain.search.infra.persist.jpa.JpaSubwayStationRepository
import com.neki.domain.search.models.LegalDong
import com.neki.domain.search.models.NearbyStation
import com.neki.domain.search.models.PhotoBoothSearch
import com.neki.domain.search.models.SubwayStation
import com.neki.domain.search.models.SubwayStationId
import com.neki.domain.search.service.qu.QueryUnderstandingService
import org.junit.jupiter.api.AfterEach
import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.GeometryFactory
import org.locationtech.jts.geom.Point
import org.locationtech.jts.geom.PrecisionModel
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * fileName       : SearchE2ETestBase
 * author         : darren
 * date           : 2026. 9. 25.
 * description    : 통합 검색 E2E 테스트를 위한 Base Class. 자동완성 원천(법정동, 역)과 부스 목록 원천(검색 색인, 지도 부스)
 */
abstract class SearchE2ETestBase : E2ETestBase() {

    @Autowired
    protected lateinit var legalDongRepository: JpaLegalDongRepository

    @Autowired
    protected lateinit var subwayStationRepository: JpaSubwayStationRepository

    @Autowired
    protected lateinit var photoBoothSearchRepository: JpaPhotoBoothSearchRepository

    @Autowired
    protected lateinit var brandRepository: JpaBrandRepository

    @Autowired
    protected lateinit var photoBoothLocationRepository: JpaPhotoBoothLocationRepository

    @Autowired
    protected lateinit var favoriteMapRepository: JpaFavoriteMapRepository

    @Autowired
    protected lateinit var userBrandOrderRepository: JpaUserBrandOrderRepository

    @Autowired
    protected lateinit var queryUnderstandingService: QueryUnderstandingService

    private val geometryFactory = GeometryFactory(PrecisionModel(), 4326)

    @AfterEach
    override fun tearDown() {
        favoriteMapRepository.deleteAllInBatch()
        userBrandOrderRepository.deleteAllInBatch()
        photoBoothLocationRepository.deleteAllInBatch()
        // 역 연결 테이블(ElementCollection)까지 지우려면 배치 삭제가 아니라 엔티티 단위로 지워야 한다
        photoBoothSearchRepository.deleteAll()
        brandRepository.deleteAllInBatch()
        legalDongRepository.deleteAllInBatch()
        subwayStationRepository.deleteAllInBatch()
        super.tearDown()
    }

    protected fun createLegalDong(code: String, level: Int, leafName: String, fullName: String): LegalDong =
        legalDongRepository.save(LegalDong(code = code, level = level, leafName = leafName, fullName = fullName))

    protected fun createSubwayStation(
        name: String,
        lineName: String,
        longitude: Double,
        latitude: Double,
    ): SubwayStation = subwayStationRepository.save(
        SubwayStation(
            id = SubwayStationId(name = name, lineName = lineName),
            location = point(longitude, latitude),
        ),
    )

    protected fun createBrand(name: String, code: String, platform: String? = null): Brand =
        brandRepository.save(Brand(name = name, code = code, platform = platform))

    /**
     * 색인 잡이 만든 것과 같은 검색 색인 행(_read). branchName 은 브랜드명 접두를 뗀 값을 준다.
     * 정규화 컬럼은 색인 잡과 같은 규칙(SearchNormalizer)으로 채운다. 역은 거리와 무관하게 주어진 역에 연결한다
     */
    protected fun createIndexedBooth(
        brand: Brand,
        idx: String,
        branchName: String,
        longitude: Double,
        latitude: Double,
        regionIds: List<String>,
        stations: List<SubwayStation> = emptyList(),
        address: String = "서울 강남구 $branchName",
    ): PhotoBoothSearch = photoBoothSearchRepository.save(
        PhotoBoothSearch(
            platform = brand.platform!!,
            idx = idx,
            brandId = brand.id!!,
            brandName = brand.name,
            brandCode = brand.code,
            branchName = branchName,
            address = address,
            location = point(longitude, latitude),
            normalizedBrandName = SearchNormalizer.normalize(brand.name),
            normalizedBranchName = SearchNormalizer.normalize(branchName),
            searchText = SearchNormalizer.searchText(brand.name, branchName, address),
            regionIds = regionIds.toTypedArray(),
            siteKey = ":$longitude,$latitude",
            sourceDt = LocalDate.of(2026, 9, 29),
            businessDate = LocalDate.of(2026, 9, 30),
            indexedAt = LocalDateTime.of(2026, 9, 30, 5, 30),
            stations = stations.map {
                NearbyStation(it.name, it.lineName, it.distanceFrom(Coordinate(longitude, latitude)))
            },
        ),
    )

    /**
     * stores-sync 가 만든 것과 같은 지도 부스(TB_PHOTO_BOOTH_LOCATION). 원천 키가 색인 행과 같다
     */
    protected fun createMapBooth(booth: PhotoBoothSearch, adminHidden: Boolean = false): PhotoBoothLocation =
        photoBoothLocationRepository.save(
            PhotoBoothLocation(
                mapId = "source:${booth.platform}:${booth.idx}",
                brandId = booth.brandId,
                branchName = booth.branchName,
                address = booth.address!!,
                location = booth.location,
                sourcePlatform = booth.platform,
                sourceIdx = booth.idx,
                adminHidden = adminHidden,
            ),
        )

    protected fun favorite(userId: Long, location: PhotoBoothLocation): FavoriteMap =
        favoriteMapRepository.save(FavoriteMap(userId = userId, locationId = location.id!!))

    protected fun orderBrands(userId: Long, vararg brands: Brand) {
        userBrandOrderRepository.saveAll(UserBrandOrder.ofOrderedBrandIds(userId, brands.map { it.id!! }))
    }

    /**
     * 강남 일대 부스. 강남구 3개 + 숨김 1개 + 지도 미동기화 1개, 서초구 1개. 모두 강남역 2호선에 딸린다.
     * 강남구청역 7호선에 딸린 부스는 없다. 사용자는 포토이즘 강남1호점을 즐겨찾기했다.
     */
    protected inner class GangnamBooths(userId: Long) {
        val photoism: Brand = createBrand("포토이즘", "PHOTOISM", "PHOTOISM")
        val lifeFourCut: Brand = createBrand("인생네컷", "LIFEFOURCUTS", "LIFE_FOUR_CUT")

        private val gangnamStation: SubwayStation = createSubwayStation("강남", "2호선", 127.0276, 37.4979)

        init {
            createSubwayStation("강남구청", "7호선", 127.0413, 37.5171)
            createLegalDong("1100000000", 1, "서울특별시", "서울특별시")
            createLegalDong("1168000000", 2, "강남구", "서울특별시 강남구")
            createLegalDong("1165000000", 2, "서초구", "서울특별시 서초구")
        }

        private val gangnamGu: List<String> = listOf("1100000000", "1168000000", "1168010100")
        private val seochoGu: List<String> = listOf("1100000000", "1165000000", "1165010800")

        val photoismGangnam1: PhotoBoothLocation = createMapBooth(
            createIndexedBooth(photoism, "p1", "강남1호점", 127.0271830, 37.5021077, gangnamGu, listOf(gangnamStation)),
        )
        val photoismGangnamStation: PhotoBoothLocation = createMapBooth(
            createIndexedBooth(photoism, "p2", "강남역점", 127.0289042, 37.4967118, gangnamGu, listOf(gangnamStation)),
        )
        val lifeFourCutGangnamStation: PhotoBoothLocation = createMapBooth(
            createIndexedBooth(lifeFourCut, "l1", "강남역점", 127.0288671, 37.5002916, gangnamGu, listOf(gangnamStation)),
        )
        val lifeFourCutSeocho: PhotoBoothLocation = createMapBooth(
            createIndexedBooth(lifeFourCut, "l2", "서초점", 127.0256833, 37.4995520, seochoGu, listOf(gangnamStation)),
        )

        init {
            createMapBooth(
                createIndexedBooth(photoism, "p3", "숨김점", 127.0280, 37.4990, gangnamGu, listOf(gangnamStation)),
                adminHidden = true,
            )
            createIndexedBooth(photoism, "p4", "미동기화점", 127.0281, 37.4991, gangnamGu, listOf(gangnamStation))
            favorite(userId, photoismGangnam1)

            // 앱이 뜰 때 올린 사전에는 이 데이터가 없으므로 다시 올린다 (운영에서는 재기동 시점)
            queryUnderstandingService.reloadDictionary()
        }
    }

    private fun point(longitude: Double, latitude: Double): Point =
        geometryFactory.createPoint(Coordinate(longitude, latitude))
}
