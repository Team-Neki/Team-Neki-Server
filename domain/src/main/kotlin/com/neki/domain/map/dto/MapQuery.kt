package com.neki.domain.map.dto

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

    /** 수집 원천 키로 포토부스 위치를 찾는다. 관리자가 숨긴 지점은 빠진다 */
    data class GetSourceLocations(val userId: Long, val sources: List<PhotoBoothSource>)
}
