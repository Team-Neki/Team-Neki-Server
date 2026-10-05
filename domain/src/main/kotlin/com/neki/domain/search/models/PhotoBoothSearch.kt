package com.neki.domain.search.models

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
import org.hibernate.annotations.Immutable
import org.locationtech.jts.geom.Point
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * fileName       : PhotoBoothSearch
 * author         : koo
 * date           : 2026. 9. 25.
 * description    : 검색 API 가 읽는 검색 카드 (_read). 쓰기는 PhotoBoothSearchWrite 가 _write 에 하고
 *                  searchIndexJob 이 두 테이블의 이름을 맞바꾼다. 컬럼은 PhotoBoothSearchWrite 와 같아야 한다
 */
@Entity
@Immutable
@Table(name = PhotoBoothSearch.TABLE)
class PhotoBoothSearch(
    // 읽기 전용이지만 _write 와 물리 DDL 이 같아야 이름을 맞바꿔도 한쪽만 identity 가 없는 일이 없다 (테스트의 H2 도 마찬가지)
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
    @CollectionTable(name = PhotoBoothSearch.STATION_TABLE, joinColumns = [JoinColumn(name = "search_id")])
    val stations: List<NearbyStation> = emptyList(),
) {
    companion object {
        const val TABLE = "tb_photo_booth_search_read"
        const val STATION_TABLE = "tb_photo_booth_search_read_station"
    }
}
