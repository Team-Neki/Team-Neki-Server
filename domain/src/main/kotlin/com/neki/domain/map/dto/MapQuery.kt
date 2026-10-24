package com.neki.domain.map.dto

import com.neki.domain.map.models.PhotoBoothKey
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

    /** 포토부스 키로 지금 세대(enriched)에 있는 부스를 찾는다. 검색이 색인 행을 지도 부스와 짝지을 때 쓴다 */
    data class GetPhotoBoothsByKeys(val userId: Long, val keys: List<PhotoBoothKey>)
}
