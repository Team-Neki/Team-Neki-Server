package com.neki.domain.map.dto

import com.neki.core.domain.vo.Pagination
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
     * 브랜드명·지점명 접두 검색. coordinate(경도 x, 위도 y)가 있으면 가까운 순, 없으면 브랜드명, 지점명 순.
     */
    data class SearchPhotoBooths(val keyword: String, val pagination: Pagination, val coordinate: Coordinate?)
}
