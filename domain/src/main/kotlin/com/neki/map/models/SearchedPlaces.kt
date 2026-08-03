package com.neki.map.models

/**
 * fileName       : SearchedPlaces
 * author         : koo
 * date           : 2026. 8. 4.
 * description    : 외부 지도 검색 결과. 어댑터가 지도 API 응답을 변환해 넘겨준다.
 */
data class SearchedPlaces(val places: List<SearchedPlace>, val pagination: SearchPagination)

data class SearchedPlace(
    val id: String,
    val placeName: String,
    val roadAddressName: String,
    val addressName: String?,
    val longitude: String, // 경도
    val latitude: String, // 위도
    val phone: String?,
    val categoryName: String?,
)

data class SearchPagination(val totalCount: Int, val pageableCount: Int, val isEnd: Boolean)
