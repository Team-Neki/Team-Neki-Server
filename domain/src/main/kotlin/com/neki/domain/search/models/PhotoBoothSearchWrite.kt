package com.neki.domain.search.models

import com.neki.domain.search.SearchNormalizer
import jakarta.persistence.CollectionTable
import jakarta.persistence.Column
import jakarta.persistence.ElementCollection
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.Table
import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.GeometryFactory
import org.locationtech.jts.geom.Point
import org.locationtech.jts.geom.PrecisionModel
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * fileName       : PhotoBoothSearchWrite
 * author         : koo
 * date           : 2026. 9. 25.
 * description    : 색인 잡이 채우는 검색 카드 (_write). 파생 필드(지점명, 정규화 문자열, region_ids, site_key, 역)는
 *                  of() 만 계산하므로 카드는 늘 같은 규칙으로 만들어진다. 컬럼은 PhotoBoothSearch 와 같아야 한다
 */
@Entity
@Table(name = PhotoBoothSearchWrite.TABLE)
class PhotoBoothSearchWrite private constructor(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    val id: Long? = null,

    @Column(name = "platform", nullable = false, length = 32)
    val platform: String,

    @Column(name = "idx", nullable = false, length = 64)
    val idx: String,

    @Column(name = "brand_id", nullable = false)
    val brandId: Long,

    @Column(name = "brand_name", nullable = false, length = 100)
    val brandName: String,

    @Column(name = "brand_code", nullable = false, length = 50)
    val brandCode: String,

    @Column(name = "branch_name", nullable = false, length = 255)
    val branchName: String,

    @Column(name = "address", length = 255)
    val address: String?,

    @Column(name = "location", nullable = false, columnDefinition = "geometry(Point, 4326)")
    val location: Point,

    @Column(name = "normalized_brand_name", nullable = false, length = 100)
    val normalizedBrandName: String,

    @Column(name = "normalized_branch_name", nullable = false, length = 255)
    val normalizedBranchName: String,

    @Column(name = "search_text", nullable = false, columnDefinition = "TEXT")
    val searchText: String,

    @Column(name = "region_ids", nullable = false)
    val regionIds: Array<String>,

    @Column(name = "site_key", nullable = false, length = 40)
    val siteKey: String,

    @Column(name = "source_dt", nullable = false)
    val sourceDt: LocalDate,

    @Column(name = "business_date", nullable = false)
    val businessDate: LocalDate,

    @Column(name = "indexed_at", nullable = false)
    val indexedAt: LocalDateTime,

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = PhotoBoothSearchWrite.STATION_TABLE, joinColumns = [JoinColumn(name = "search_id")])
    val stations: List<NearbyStation> = emptyList(),
) {
    companion object {
        const val TABLE = "tb_photo_booth_search_write"
        const val STATION_TABLE = "tb_photo_booth_search_write_station"

        private val GEOMETRY_FACTORY = GeometryFactory(PrecisionModel(), 4326)

        /**
         * enriched 한 행을 카드로. 좌표가 없는 행은 카드가 될 수 없으므로 호출 전에 걸러야 한다.
         * 브랜드는 map 도메인 것이라 엔티티 대신 값만 받는다.
         */
        fun of(
            enriched: PhotoBoothEnriched,
            brandId: Long,
            brandName: String,
            brandCode: String,
            stations: List<SubwayStation>,
            businessDate: LocalDate,
            indexedAt: LocalDateTime,
        ): PhotoBoothSearchWrite {
            val coordinate: Coordinate = requireNotNull(enriched.coordinateOrNull()) {
                "좌표가 없는 지점은 카드로 만들 수 없습니다 (${enriched.id.platform}/${enriched.id.idx})"
            }
            val branchName: String = SearchNormalizer.branchName(brandName, enriched.name)
            return PhotoBoothSearchWrite(
                platform = enriched.id.platform,
                idx = enriched.id.idx,
                brandId = brandId,
                brandName = brandName,
                brandCode = brandCode,
                branchName = branchName,
                address = enriched.address,
                location = GEOMETRY_FACTORY.createPoint(coordinate),
                normalizedBrandName = SearchNormalizer.normalize(brandName),
                normalizedBranchName = SearchNormalizer.normalize(branchName),
                searchText = SearchNormalizer.searchText(brandName, branchName, enriched.address),
                regionIds = SearchNormalizer.regionIds(enriched.bCode).toTypedArray(),
                siteKey = SearchNormalizer.siteKey(enriched.bCode, coordinate.x, coordinate.y),
                sourceDt = enriched.sourceDt,
                businessDate = businessDate,
                indexedAt = indexedAt,
                stations = NearbyStation.within(stations, coordinate),
            )
        }
    }
}
