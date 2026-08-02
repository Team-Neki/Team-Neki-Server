package com.neki.map.application.dto

import com.neki.map.application.port.dto.MapContract

/**
 * fileName       : MapResult
 * author         : koo
 * date           : 2026. 7. 21.
 * description    : Map domain result
 */
object MapResult {
    data class GetBrand(val id: Long, val name: String, val code: String, val storageKey: String?)

    data class CollectPhotoBooth(val collectedCount: Int, val duplicatedCount: Int, val totalProcessed: Int)

    data class PhotoBooth(val x1: Double, val y1: Double, val x2: Double, val y2: Double)

    data class GetPolygonLocation(
        val locations: List<MapContract.PhotoBoothLocation>,
        val favoriteLocationIds: Set<Long>,
    )

    data class GetPointLocation(
        val locations: List<MapContract.PhotoBoothLocationWithDistance>,
        val favoriteLocationIds: Set<Long>,
    )

    data class GetFavoriteMap(val locations: List<MapContract.PhotoBoothLocation>)
}
