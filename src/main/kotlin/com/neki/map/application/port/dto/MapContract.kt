package com.neki.map.application.port.dto

import org.locationtech.jts.geom.Point

/**
 * fileName       : MapContract
 * author         : koo
 * date           : 2026. 7. 22.
 * description    : Map domain port 계약 타입
 */
object MapContract {
    /**
     * 외부 지도 검색 API 응답
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
     * 포토부스 위치 조회 프로젝션
     */
    data class PhotoBoothLocation(
        val id: Long,
        val brandName: String,
        val branchName: String,
        val address: String,
        val location: Point,
    )

    /**
     * 거리 정보를 포함한 포토부스 위치 조회 프로젝션
     */
    data class PhotoBoothLocationWithDistance(
        val id: Long,
        val brandName: String,
        val branchName: String,
        val address: String,
        val location: Point,
        val distance: Int,
    )
}
