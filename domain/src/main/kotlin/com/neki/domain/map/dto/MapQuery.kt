package com.neki.domain.map.dto

import com.neki.core.domain.vo.Pagination
import com.neki.domain.map.models.PhotoBoothSource
import org.locationtech.jts.geom.Coordinate

/**
 * fileName       : MapQuery
 * author         : koo
 * date           : 2026. 7. 21.
 * description    : Map domain query
 */
object MapQuery {
    data class GetBrand(val userId: Long)

    data class GetPolygonLocation(val userId: Long, val coordinates: List<Coordinate>, val brandIds: List<Long>?)

    data class GetPointLocation(
        val userId: Long,
        val coordinate: Coordinate,
        val radiusInMeters: Int = 1000,
        val brandIds: List<Long>?,
    )

    data class GetFavoriteMaps(val userId: Long)

    /**
     * 부스 검색. keyword 전체가 이름 앞부분이거나, terms 의 낱말마다 브랜드명·지점명·주소 중 하나에 들어 있는 부스.
     * terms 는 낱말마다 같은 뜻의 이름 묶음이다 (`서울특별시` ↔ `서울`). 정렬은 얼마나 잘 맞는지 순,
     * 그 안에서 coordinate(경도 x, 위도 y)가 있으면 가까운 순, 없으면 브랜드명, 지점명 순.
     */
    data class SearchPhotoBooths(
        val keyword: String,
        val terms: List<List<String>>,
        val pagination: Pagination,
        val coordinate: Coordinate?,
    )

    /** 수집 원천 키로 포토부스 위치를 찾는다. 관리자가 숨긴 지점은 빠진다 */
    data class GetSourceLocations(val userId: Long, val sources: List<PhotoBoothSource>)
}
