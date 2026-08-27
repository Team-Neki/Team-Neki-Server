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
     * 다각형 영역에서 사용할 수 있는 지도 필터 목록.
     * 필터가 늘어나면 배열이 아닌 이 타입에 필드를 추가한다.
     */
    data class PolygonFilter(val brandFilter: List<BrandFilter>)

    /**
     * 로고 이미지는 브랜드 전체 조회(GET /api/photo-booths/brand)에서 이미 내려주므로 여기서는 싣지 않는다.
     */
    data class BrandFilter(val id: Long, val name: String, val code: String, val count: Long)

    data class CollectPhotoBooth(val collectedCount: Int, val duplicatedCount: Int, val totalProcessed: Int)

    data class PhotoBooth(val x1: Double, val y1: Double, val x2: Double, val y2: Double)

    data class GetPolygonLocation(val locations: List<PhotoBoothLocationView>, val favoriteLocationIds: Set<Long>)

    data class GetPointLocation(
        val locations: List<PhotoBoothLocationWithDistance>,
        val favoriteLocationIds: Set<Long>,
    )

    data class GetFavoriteMap(val locations: List<PhotoBoothLocationView>)
}
