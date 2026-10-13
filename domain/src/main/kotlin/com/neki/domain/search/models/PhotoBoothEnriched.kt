package com.neki.domain.search.models

import jakarta.persistence.Column
import jakarta.persistence.Embeddable
import jakarta.persistence.EmbeddedId
import jakarta.persistence.Entity
import jakarta.persistence.Table
import org.hibernate.annotations.Immutable
import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.Point
import java.io.Serializable
import java.time.LocalDate

/**
 * fileName       : PhotoBoothEnriched
 * author         : koo
 * date           : 2026. 9. 25.
 * description    : Workflow 가 소유하는 tb_photo_booth_enriched 의 읽기 전용 매핑. 색인 입력 계약(BACKEND-64) 7열만 안다. 좌표는 location 한 열 (BACKEND-244)
 */
@Entity
@Immutable
@Table(name = "tb_photo_booth_enriched")
class PhotoBoothEnriched(
    @EmbeddedId
    val id: PhotoBoothEnrichedId,

    @Column(name = "name", nullable = false)
    val name: String,

    @Column(name = "address")
    val address: String?,

    @Column(name = "location", columnDefinition = "geometry(Point, 4326)")
    val location: Point?,

    @Column(name = "source_dt", nullable = false)
    val sourceDt: LocalDate,

    @Column(name = "b_code", columnDefinition = "CHAR(10)")
    val bCode: String?,
) {
    /** 좌표가 없으면 카드를 만들 수 없다 */
    fun coordinateOrNull(): Coordinate? = location?.coordinate
}

@Embeddable
data class PhotoBoothEnrichedId(
    @Column(name = "platform", nullable = false)
    val platform: String,

    @Column(name = "idx", nullable = false)
    val idx: String,
) : Serializable
