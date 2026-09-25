package com.neki.api.search.application.dto

/**
 * fileName       : SearchResult
 * author         : koo
 * date           : 2026. 9. 15.
 * description    : Search domain result
 */
object SearchResult {
    /**
     * 검색 자동완성. 지역·역·부스 세 탭이 같은 모양을 내려준다.
     * keyword 는 화면에 그대로 보여 주고 고른 값을 부스 목록 요청에 넘기는 문자열이다.
     */
    data class Completion(val keywords: List<String>, val hasNext: Boolean, val totalCount: Long)

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
