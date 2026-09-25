package com.neki.domain.search.service

import com.neki.domain.search.SearchNormalizer
import com.neki.domain.search.client.BrandClient
import com.neki.domain.search.models.NearbyStation
import com.neki.domain.search.models.PhotoBoothEnriched
import com.neki.domain.search.models.PhotoBoothSearch
import com.neki.domain.search.models.SearchBrand
import com.neki.domain.search.models.SubwayStation
import com.neki.domain.search.models.UserLocation
import com.neki.domain.search.repository.PhotoBoothSearchRepository
import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.GeometryFactory
import org.locationtech.jts.geom.PrecisionModel
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * fileName       : SearchIndexService
 * author         : koo
 * date           : 2026. 9. 25.
 * description    : tb_photo_booth_enriched 8열을 검색 카드로 전량 재생성한다. 외부 호출 없이 DB 만 읽고 쓴다
 */
@Service
class SearchIndexService(private val repository: PhotoBoothSearchRepository, private val brandClient: BrandClient) {

    private val log = LoggerFactory.getLogger(javaClass)

    private val geometryFactory = GeometryFactory(PrecisionModel(), 4326)

    /**
     * 카드가 0건이거나 직전 카드 수의 절반 미만이면 예외로 끝내고 직전 카드를 그대로 둔다.
     * DELETE 와 INSERT 가 한 트랜잭션이라 중간에 죽어도 직전 카드가 남는다.
     */
    @Transactional
    fun rebuild(businessDate: LocalDate): SearchIndexResult {
        val brands: Map<String, SearchBrand> = brandClient.findAll()
            .filter { it.platform != null }
            .associateBy { it.platform!! }
        val enriched: List<PhotoBoothEnriched> = repository.findAllEnriched()
        val stations: List<SubwayStation> = repository.findAllStations()
        val indexedAt = LocalDateTime.now()

        val skippedNoBrand = mutableMapOf<String, Int>()
        var skippedNoCoordinate = 0
        val cards: List<PhotoBoothSearch> = enriched.mapNotNull { row ->
            val brand: SearchBrand? = brands[row.id.platform]
            if (brand == null) {
                skippedNoBrand.merge(row.id.platform, 1, Int::plus)
                return@mapNotNull null
            }
            val longitude: Double? = row.longitude
            val latitude: Double? = row.latitude
            if (longitude == null || latitude == null) {
                skippedNoCoordinate++
                return@mapNotNull null
            }
            toCard(row, brand, longitude, latitude, stations, businessDate, indexedAt)
        }

        val previous: Long = repository.count()
        check(cards.isNotEmpty()) { "색인할 지점이 없습니다. enriched 가 비어 있거나 전부 건너뛰었습니다 (직전 카드 ${previous}건)" }
        check(cards.size * 2 >= previous) { "색인 건수 ${cards.size}건이 직전 카드 ${previous}건의 절반 미만이라 교체하지 않습니다" }

        repository.replaceAll(cards)

        skippedNoBrand.forEach { (platform, count) ->
            log.warn("tb_brand.platform 에 없는 platform 이라 건너뜀 (platform={}, count={})", platform, count)
        }
        val result = SearchIndexResult(
            indexed = cards.size,
            stationLinks = cards.sumOf { it.stations.size },
            skippedNoCoordinate = skippedNoCoordinate,
            skippedNoBrand = skippedNoBrand,
        )
        log.info("검색 카드 재생성 완료 (businessDate={}, previous={}, result={})", businessDate, previous, result)
        return result
    }

    private fun toCard(
        row: PhotoBoothEnriched,
        brand: SearchBrand,
        longitude: Double,
        latitude: Double,
        stations: List<SubwayStation>,
        businessDate: LocalDate,
        indexedAt: LocalDateTime,
    ): PhotoBoothSearch {
        val branchName: String = SearchNormalizer.branchName(brand.name, row.name)
        val here = UserLocation(latitude, longitude)
        // ponytail: 카드 x 역 전수 비교. 수천 x 수천이면 충분하고, 그 이상이면 PostGIS ST_DWithin 으로 올린다
        val nearby: List<NearbyStation> = stations.mapNotNull { station ->
            val distance: Int = here.distanceTo(station.location.y, station.location.x)
            NearbyStation(station.id.name, station.id.lineName, distance).takeIf { distance <= STATION_RADIUS_METERS }
        }
        return PhotoBoothSearch(
            platform = row.id.platform,
            idx = row.id.idx,
            brandId = brand.id,
            brandName = brand.name,
            brandCode = brand.code,
            branchName = branchName,
            address = row.address,
            location = geometryFactory.createPoint(Coordinate(longitude, latitude)),
            normalizedBrandName = SearchNormalizer.normalize(brand.name),
            normalizedBranchName = SearchNormalizer.normalize(branchName),
            searchText = SearchNormalizer.searchText(brand.name, branchName, row.address),
            regionIds = SearchNormalizer.regionIds(row.bCode).toTypedArray(),
            siteKey = SearchNormalizer.siteKey(row.bCode, longitude, latitude),
            sourceDt = row.sourceDt,
            businessDate = businessDate,
            indexedAt = indexedAt,
            stations = nearby,
        )
    }

    companion object {
        private const val STATION_RADIUS_METERS = 1000
    }
}

/**
 * 재생성 결과. 잡이 로그로 남긴다
 */
data class SearchIndexResult(
    val indexed: Int,
    val stationLinks: Int,
    val skippedNoCoordinate: Int,
    val skippedNoBrand: Map<String, Int>,
)
