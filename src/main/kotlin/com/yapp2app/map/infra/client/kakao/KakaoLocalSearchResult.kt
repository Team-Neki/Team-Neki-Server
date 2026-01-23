package com.yapp2app.map.infra.client.kakao

import com.fasterxml.jackson.annotation.JsonProperty

/**
 * fileName       : KakaoMapDto
 * author         : darren
 * date           : 2026. 01. 13.
 * description    : Kakao Map API 키워드 검색 응답 DTO
 */

/**
 * 카카오 맵 수집 데이터 DTO
 */
data class KakaoLocalSearchResult(
    @JsonProperty("documents")
    val documents: List<KakaoPlace>,

    @JsonProperty("meta")
    val meta: KakaoPaginationMeta,

    ) {
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

    data class KakaoPaginationMeta(
        @JsonProperty("total_count")
        val totalCount: Int,

        @JsonProperty("pageable_count")
        val pageableCount: Int,

        @JsonProperty("is_end")
        val isEnd: Boolean,
    )
}
