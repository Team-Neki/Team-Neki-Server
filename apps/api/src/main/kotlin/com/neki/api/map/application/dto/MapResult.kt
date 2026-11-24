package com.neki.api.map.application.dto

import com.neki.domain.map.models.PhotoBoothKey
import com.neki.domain.map.models.PhotoBoothView
import com.neki.domain.map.models.PhotoBoothWithDistance

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

    // ── 지도 V2 (포토부스 키) ─────────────────────────────────────────────────

    data class GetPolygonPhotoBooths(val photoBooths: List<PhotoBoothView>, val favoriteKeys: Set<PhotoBoothKey>)

    data class GetPointPhotoBooths(val photoBooths: List<PhotoBoothWithDistance>, val favoriteKeys: Set<PhotoBoothKey>)

    data class GetFavoritePhotoBooths(val photoBooths: List<PhotoBoothView>)

    data class GetPhotoBoothsByKeys(val photoBooths: List<PhotoBooth>) {
        /** 지금 세대에 있는 부스 키와 사용자 즐겨찾기 여부 */
        data class PhotoBooth(val key: PhotoBoothKey, val favorite: Boolean)
    }
}
