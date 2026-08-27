package com.neki.domain.map.dto

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

    data class PolygonFilter(val userId: Long, val coordinates: List<Coordinate>, val brandIds: List<Long>?)

    data class GetPointLocation(
        val userId: Long,
        val coordinate: Coordinate,
        val radiusInMeters: Int = 1000,
        val brandIds: List<Long>?,
    )

    data class GetFavoriteMaps(val userId: Long)
}
