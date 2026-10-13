package com.neki.domain.map.models

import jakarta.persistence.Column
import jakarta.persistence.EmbeddedId
import jakarta.persistence.Entity
import jakarta.persistence.Table
import org.hibernate.annotations.Immutable
import org.locationtech.jts.geom.Point

/**
 * fileName       : EnrichedPhotoBooth
 * author         : darren
 * date           : 2026. 10. 13.
 * description    : 지도 V2 가 읽는 포토부스. Workflow 가 소유하는 tb_photo_booth_enriched 의 읽기 전용 매핑이다.
 *   search 도메인의 PhotoBoothEnriched 와 같은 테이블이지만 도메인 격리를 위해 map 이 따로 매핑하고, 계약 7열 중 지도에 필요한 열만 안다.
 */
@Entity
@Immutable
@Table(name = "tb_photo_booth_enriched")
class EnrichedPhotoBooth(
    @EmbeddedId
    val key: PhotoBoothKey,

    /** 수집한 지점명 원문 (브랜드명 접두 포함, 예: 포토이즘 강남점) */
    @Column(name = "name", nullable = false)
    val name: String,

    @Column(name = "address")
    val address: String?,

    /** 좌표를 얻지 못한 지점은 null 이고 지도에 나오지 않는다 */
    @Column(name = "location", columnDefinition = "geometry(Point, 4326)")
    val location: Point?,
)
