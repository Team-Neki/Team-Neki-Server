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
    val brandId: Long,
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

/**
 * 다각형 영역 조회 결과 묶음.
 *
 * 필터는 이 컬렉션에서 축별로 집계한다. 필터가 늘어나도 조회는 한 번만 하고
 * 집계 축만 이 클래스에 메서드로 추가하면 되므로 DB 왕복이 축 개수만큼 늘지 않는다.
 */
class PhotoBoothLocations(private val locations: List<PhotoBoothLocationView>) {

    /**
     * 영역 내에 포토부스가 존재하는 브랜드만 key 로 갖는다.
     */
    fun countByBrandId(): Map<Long, Long> = locations
        .groupingBy { it.brandId }
        .eachCount()
        .mapValues { (_, count) -> count.toLong() }
}
