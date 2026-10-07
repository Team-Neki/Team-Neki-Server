package com.neki.domain.map.models

import com.neki.core.domain.BaseTimeEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.hibernate.annotations.DynamicUpdate
import org.locationtech.jts.geom.Point

/**
 * fileName       : PhotoBoothLocation
 * author         : darren
 * date           : 2026. 01. 13.
 * description    : 포토부스 위치 정보 엔티티
 */
@Entity
@Table(name = "TB_PHOTO_BOOTH_LOCATION")
@DynamicUpdate
class PhotoBoothLocation(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(name = "map_id", nullable = false, length = 128)
    val mapId: String,

    @Column(name = "brand_id", nullable = false)
    var brandId: Long,

    @Column(name = "branch_name", nullable = false)
    var branchName: String,

    @Column(name = "address", nullable = false, length = 255)
    var address: String,

    @Column(name = "location", nullable = false, columnDefinition = "geometry(Point, 4326)")
    var location: Point,

    /** 수집 원천 platform. source_idx 와 함께 세대를 넘어 안정적인 원천 키 (V34). 카카오 수집 지점은 null, 동기화된 관리자 등록 지점은 브랜드 platform (V36) */
    @Column(name = "source_platform", length = 32)
    val sourcePlatform: String? = null,

    @Column(name = "source_idx", length = 64)
    val sourceIdx: String? = null,

    /** 관리자 노출 중지. 수집 동기화(stores-sync)는 이 값을 바꾸지 않는다 */
    @Column(name = "admin_hidden", nullable = false)
    val adminHidden: Boolean = false,
) : BaseTimeEntity() {

    /**
     * 외부 지도 API로 수집한 최신 정보로 갱신한다. mapId는 식별자이므로 바뀌지 않는다.
     */
    fun refreshPlace(brandId: Long, branchName: String, address: String, location: Point) {
        this.brandId = brandId
        this.branchName = branchName
        this.address = address
        this.location = location
    }

    /** 원천 키. 카카오 수집 지점과 어드민이 직접 넣은 지점은 없다. TB_PHOTO_BOOTH_MANUAL 에서 동기화된 지점은 (platform, manual-<id>) */
    fun source(): PhotoBoothSource? =
        if (sourcePlatform != null && sourceIdx != null) PhotoBoothSource(sourcePlatform, sourceIdx) else null
}
