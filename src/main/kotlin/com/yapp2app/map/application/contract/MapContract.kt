package com.yapp2app.map.application.contract

import com.fasterxml.jackson.annotation.JsonProperty
import org.locationtech.jts.geom.Point

/**
 * fileName       : KakaoLocalSearchResponse
 * author         : darren
 * date           : 2026. 01. 13.
 * description    : Kakao Local API 키워드 검색 응답 DTO
 */

/**
 * 브랜드 조회 QueryDsl DTO
 */
data class BrandDto(val id: Long, val name: String, val code: String, val storageKey: String?)

/**
 * 카카오 맵 수집 데이터 DTO
 */
data class KakaoLocalSearchResponse(
    @JsonProperty("documents")
    val documents: List<KakaoPlace>,

    @JsonProperty("meta")
    val meta: KakaoMeta,
)

/**
 * 카카오 맵 수집 데이터 DTO
 */
data class KakaoPlace(
    @JsonProperty("id")
    val id: String,

    @JsonProperty("place_name")
    val placeName: String,

    @JsonProperty("road_address_name")
    val roadAddressName: String,

    @JsonProperty("address_name")
    val addressName: String?,

    @JsonProperty("x")
    val longitude: String, // 경도

    @JsonProperty("y")
    val latitude: String, // 위도

    @JsonProperty("phone")
    val phone: String?,

    @JsonProperty("category_name")
    val categoryName: String?,
)

/**
 * 카카오 맵 수집 데이터 DTO
 */
data class KakaoMeta(
    @JsonProperty("total_count")
    val totalCount: Int,

    @JsonProperty("pageable_count")
    val pageableCount: Int,

    @JsonProperty("is_end")
    val isEnd: Boolean,
)

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
