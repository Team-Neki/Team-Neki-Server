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
 * 조회는 폴리곤 조건만으로 한 번 하고, 필터 적용과 축별 집계는 모두 이 컬렉션이 담당한다.
 * 필터가 늘어나도 DB 왕복이 축 개수만큼 늘지 않고 이 클래스에 메서드만 추가하면 된다.
 */
class PhotoBoothLocations(private val locations: List<PhotoBoothLocationView>) {

    /**
     * brandIds 가 비어있으면(null 또는 []) 모든 브랜드를 의미하므로 그대로 둔다.
     */
    fun filterByBrandIds(brandIds: List<Long>?): PhotoBoothLocations {
        if (brandIds.isNullOrEmpty()) return this

        val targetBrandIds: Set<Long> = brandIds.toSet()

        return PhotoBoothLocations(locations.filter { it.brandId in targetBrandIds })
    }

    /**
     * 영역 내에 포토부스가 존재하는 브랜드만 key 로 갖는다.
     */
    fun countByBrandId(): Map<Long, Long> = locations
        .groupingBy { it.brandId }
        .eachCount()
        .mapValues { (_, count) -> count.toLong() }

    fun toList(): List<PhotoBoothLocationView> = locations
}
