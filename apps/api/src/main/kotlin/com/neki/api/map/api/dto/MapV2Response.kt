package com.neki.api.map.api.dto

import io.swagger.v3.oas.annotations.media.Schema

/**
 * fileName       : MapV2Response
 * author         : darren
 * date           : 2026. 10. 13.
 * description    : 지도 V2 응답 DTO. V1 과 모양이 같고 부스 id 만 포토부스 키 문자열이다
 */
object MapV2Response {
    private const val PHOTO_BOOTH_ID_DESCRIPTION =
        "포토부스 ID (`source:<브랜드 코드>:<지점 ID>`). 즐겨찾기 API 의 path 와 검색 부스 목록의 id 와 같은 값"
    private const val PHOTO_BOOTH_ID_EXAMPLE = "source:LIFEFOURCUTS:1768232303"

    @Schema(name = "GetPolygonPhotoBoothsV2Response")
    data class GetPolygonPhotoBooths(
        @field:Schema(description = "포토부스 목록")
        val items: List<Item>,
    ) {
        @Schema(name = "PhotoBoothV2Info")
        data class Item(
            @field:Schema(description = PHOTO_BOOTH_ID_DESCRIPTION, example = PHOTO_BOOTH_ID_EXAMPLE)
            val id: String,

            @field:Schema(description = "브랜드 이름", example = "인생네컷")
            val brandName: String,

            @field:Schema(description = "포토부스 이름", example = "인생네컷 강남역점")
            val branchName: String,

            @field:Schema(description = "주소. 수집하지 못했으면 빈 문자열", example = "서울 강남구 강남대로 지하 396")
            val address: String,

            @field:Schema(description = "경도", example = "127.027456")
            val longitude: Double,

            @field:Schema(description = "위도", example = "37.497946")
            val latitude: Double,

            @field:Schema(description = "즐겨찾기 여부", example = "true")
            val favorite: Boolean,
        )
    }

    @Schema(name = "GetFavoritePhotoBoothsV2Response")
    data class GetFavoritePhotoBooths(
        @field:Schema(description = "즐겨찾기한 포토부스 목록 (최근 즐겨찾기한 순)")
        val items: List<Item>,
    ) {
        @Schema(name = "FavoritePhotoBoothV2Info")
        data class Item(
            @field:Schema(description = PHOTO_BOOTH_ID_DESCRIPTION, example = PHOTO_BOOTH_ID_EXAMPLE)
            val id: String,

            @field:Schema(description = "브랜드 이름", example = "인생네컷")
            val brandName: String,

            @field:Schema(description = "포토부스 이름", example = "인생네컷 강남역점")
            val branchName: String,

            @field:Schema(description = "주소. 수집하지 못했으면 빈 문자열", example = "서울 강남구 강남대로 지하 396")
            val address: String,

            @field:Schema(description = "경도", example = "127.027456")
            val longitude: Double,

            @field:Schema(description = "위도", example = "37.497946")
            val latitude: Double,
        )
    }

    @Schema(name = "GetPointPhotoBoothsV2Response")
    data class GetPointPhotoBooths(
        @field:Schema(description = "포토부스 목록 (거리순 정렬)")
        val items: List<Item>,
    ) {
        @Schema(name = "PhotoBoothWithDistanceV2Info")
        data class Item(
            @field:Schema(description = PHOTO_BOOTH_ID_DESCRIPTION, example = PHOTO_BOOTH_ID_EXAMPLE)
            val id: String,

            @field:Schema(description = "브랜드 이름", example = "인생네컷")
            val brandName: String,

            @field:Schema(description = "포토부스 이름", example = "인생네컷 강남역점")
            val branchName: String,

            @field:Schema(description = "주소. 수집하지 못했으면 빈 문자열", example = "서울 강남구 강남대로 지하 396")
            val address: String,

            @field:Schema(description = "경도", example = "127.027456")
            val longitude: Double,

            @field:Schema(description = "위도", example = "37.497946")
            val latitude: Double,

            @field:Schema(description = "기준점으로부터의 거리 (미터)", example = "200")
            val distance: Int,

            @field:Schema(description = "즐겨찾기 여부", example = "true")
            val favorite: Boolean,
        )
    }
}
