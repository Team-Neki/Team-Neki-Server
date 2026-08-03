package com.neki.map.models

import com.neki.common.domain.BaseTimeEntity
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

    @Column(name = "map_id", nullable = false)
    val mapId: String,

    @Column(name = "brand_id", nullable = false)
    var brandId: Long,

    @Column(name = "branch_name", nullable = false, length = 100)
    var branchName: String,

    @Column(name = "address", nullable = false, length = 255)
    var address: String,

    @Column(name = "location", nullable = false, columnDefinition = "geometry(Point, 4326)")
    var location: Point,
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
}
