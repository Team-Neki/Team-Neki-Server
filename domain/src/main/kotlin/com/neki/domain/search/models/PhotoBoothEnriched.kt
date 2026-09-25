package com.neki.domain.search.models

import jakarta.persistence.Column
import jakarta.persistence.Embeddable
import jakarta.persistence.EmbeddedId
import jakarta.persistence.Entity
import jakarta.persistence.Table
import org.hibernate.annotations.Immutable
import java.io.Serializable
import java.time.LocalDate

/**
 * fileName       : PhotoBoothEnriched
 * author         : koo
 * date           : 2026. 9. 25.
 * description    : Workflow 가 소유하는 tb_photo_booth_enriched 의 읽기 전용 매핑. 색인 입력 계약(BACKEND-64) 8열만 안다
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

    @Column(name = "longitude")
    val longitude: Double?,

    @Column(name = "latitude")
    val latitude: Double?,

    @Column(name = "source_dt", nullable = false)
    val sourceDt: LocalDate,

    @Column(name = "b_code", columnDefinition = "CHAR(10)")
    val bCode: String?,
)

@Embeddable
data class PhotoBoothEnrichedId(
    @Column(name = "platform", nullable = false)
    val platform: String,

    @Column(name = "idx", nullable = false)
    val idx: String,
) : Serializable
