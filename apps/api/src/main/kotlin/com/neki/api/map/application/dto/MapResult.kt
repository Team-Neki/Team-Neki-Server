package com.neki.api.map.application.dto

import com.neki.domain.map.models.PhotoBoothLocationView
import com.neki.domain.map.models.PhotoBoothLocationWithDistance

/**
 * fileName       : MapResult
 * author         : koo
 * date           : 2026. 7. 21.
 * description    : Map domain result
 */
object MapResult {
    data class GetBrand(val id: Long, val name: String, val code: String, val storageKey: String?)

    /**
     * 다각형 영역 내 브랜드 조회 전용.
     * 로고 이미지는 브랜드 전체 조회(GET /api/photo-booths/brand)에서 이미 내려주므로 여기서는 싣지 않는다.
     */
    data class GetPolygonBrand(val id: Long, val name: String, val code: String, val boothCount: Long)

    data class CollectPhotoBooth(val collectedCount: Int, val duplicatedCount: Int, val totalProcessed: Int)

    data class PhotoBooth(val x1: Double, val y1: Double, val x2: Double, val y2: Double)

    data class GetPolygonLocation(val locations: List<PhotoBoothLocationView>, val favoriteLocationIds: Set<Long>)

    data class GetPointLocation(
        val locations: List<PhotoBoothLocationWithDistance>,
        val favoriteLocationIds: Set<Long>,
    )

    data class GetFavoriteMap(val locations: List<PhotoBoothLocationView>)
}
