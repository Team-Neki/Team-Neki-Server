package com.neki.domain.search.models

import jakarta.persistence.CollectionTable
import jakarta.persistence.Column
import jakarta.persistence.ElementCollection
import jakarta.persistence.Embeddable
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.Table
import org.locationtech.jts.geom.Point
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * fileName       : PhotoBoothSearch
 * author         : koo
 * date           : 2026. 9. 25.
 * description    : 검색 카드 엔티티. searchIndexJob 이 tb_photo_booth_enriched 에서 전량 재생성한다
 */
@Entity
@Table(name = "tb_photo_booth_search")
class PhotoBoothSearch(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
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

    // Hibernate 가 Array<String> 을 PostgreSQL varchar[] 로 바인딩한다 (V33 의 VARCHAR(10)[])
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

    // 별도 엔티티가 아니라 컬렉션이라 부모와 함께 INSERT 되고 복합키 merge-select 가 없다
    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "tb_photo_booth_search_station", joinColumns = [JoinColumn(name = "search_id")])
    val stations: List<NearbyStation> = emptyList(),
)

/**
 * 카드에서 1km 안에 있는 지하철역
 */
@Embeddable
class NearbyStation(
    @Column(name = "station_name", nullable = false, length = 60)
    val stationName: String,

    @Column(name = "line_name", nullable = false, length = 40)
    val lineName: String,

    @Column(name = "distance_m", nullable = false)
    val distanceM: Int,
)
