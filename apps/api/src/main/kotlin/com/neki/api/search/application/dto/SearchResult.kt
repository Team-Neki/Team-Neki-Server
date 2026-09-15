package com.neki.api.search.application.dto

/**
 * fileName       : SearchResult
 * author         : koo
 * date           : 2026. 9. 15.
 * description    : Search domain result
 */
object SearchResult {
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
