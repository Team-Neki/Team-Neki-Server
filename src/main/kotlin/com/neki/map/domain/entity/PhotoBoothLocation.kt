package com.neki.map.domain.entity

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
) : BaseTimeEntity()
