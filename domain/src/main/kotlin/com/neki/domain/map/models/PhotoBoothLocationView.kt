package com.neki.domain.map.models

import org.locationtech.jts.geom.Point

/**
 * fileName       : PhotoBoothLocationView
 * author         : koo
 * date           : 2026. 8. 4.
 * description    : 브랜드명을 붙인 포토부스 위치 조회 결과
 */
data class PhotoBoothLocationView(
    val id: Long,
    val brandName: String,
    val branchName: String,
    val address: String,
    val location: Point,
)

/**
 * 기준 좌표로부터의 거리를 함께 담은 조회 결과
 */
data class PhotoBoothLocationWithDistance(
    val id: Long,
    val brandName: String,
    val branchName: String,
    val address: String,
    val location: Point,
    val distance: Int,
)
