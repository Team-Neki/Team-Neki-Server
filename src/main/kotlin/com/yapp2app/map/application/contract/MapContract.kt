package com.yapp2app.map.application.contract

import org.locationtech.jts.geom.Point

/**
 * fileName       : MapContract
 * author         : darren
 * date           : 2026. 01. 13.
 * description    :
 */

/**
 * 맵 수집 데이터 DTO
 */
data class LocalSearchResult(val documents: List<Place>, val searchPaginationMeta: SearchPaginationMeta) {
    data class Place(
        val id: String,
        val placeName: String,
        val roadAddressName: String,
        val addressName: String?,
        val longitude: String, // 경도
        val latitude: String, // 위도
        val phone: String?,
        val categoryName: String?,
    )

    data class SearchPaginationMeta(val totalCount: Int, val pageableCount: Int, val isEnd: Boolean)
}

/**
 * 포토부스 위치 QueryDsl DTO
 */
data class PhotoBoothLocationDto(
    val id: Long,
    val brandId: Long,
    val name: String,
    val address: String,
    val location: Point,
)

/**
 * 거리 정보를 포함한 포토부스 위치 QueryDsl DTO
 */
data class PhotoBoothLocationWithDistanceDto(
    val id: Long,
    val brandId: Long,
    val name: String,
    val address: String,
    val location: Point,
    val distance: Int,
)
