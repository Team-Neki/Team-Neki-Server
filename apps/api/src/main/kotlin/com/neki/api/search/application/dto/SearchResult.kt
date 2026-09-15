package com.neki.api.search.application.dto

import com.neki.domain.search.models.RegionLevel

/**
 * fileName       : SearchResult
 * author         : koo
 * date           : 2026. 9. 15.
 * description    : Search domain result
 */
object SearchResult {
    data class SearchRegions(val items: List<Item>, val hasNext: Boolean, val totalCount: Long) {
        data class Item(val code: String, val level: RegionLevel, val name: String, val fullName: String)
    }

    data class SearchStations(val items: List<Item>, val hasNext: Boolean, val totalCount: Long) {
        data class Item(val name: String, val lineName: String)
    }

    /**
     * 부스 검색. 지도에 필요한 값이 다 들어 있어 고른 뒤 추가 호출이 없다.
     */
    data class SearchPhotoBooths(val items: List<GetPhotoBooths.Item>, val hasNext: Boolean, val totalCount: Long)

    data class GetPhotoBooths(val items: List<Item>) {
        data class Item(
            val id: Long,
            val brandName: String,
            val brandCode: String,
            val branchName: String,
            val address: String,
            val latitude: Double,
            val longitude: Double,
            val distance: Int?,
            val favorite: Boolean,
        )
    }

    data class GetFilter(val brandFilter: List<BrandFilter>) {
        data class BrandFilter(val id: Long, val name: String, val code: String, val count: Int)
    }
}
