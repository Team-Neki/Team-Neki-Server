package com.neki.map.api.dto

import io.swagger.v3.oas.annotations.media.Schema

/**
 * fileName       : MapResponse
 * author         : koo
 * date           : 2026. 7. 21.
 * description    : Map 관련 응답 DTO
 */
object MapResponse {
    @Schema(name = "GetBrandResponse")
    data class GetBrand(
        @field:Schema(description = "브랜드 ID", example = "1")
        val id: Long,

        @field:Schema(description = "브랜드 이름", example = "포토이즘")
        val name: String,

        @field:Schema(description = "브랜드 코드", example = "PHOTOISM")
        val code: String,

        @field:Schema(description = "브랜드 이미지", example = "https://domain/file/image/xxx.png")
        val imageUrl: String?,
    )

    @Schema(name = "CollectPhotoBoothResponse")
    data class CollectPhotoBooth(
        @field:Schema(description = "수집된 포토부스 수", example = "45")
        val collectedCount: Int,

        @field:Schema(description = "중복으로 스킵된 수", example = "5")
        val duplicatedCount: Int,

        @field:Schema(description = "총 처리된 수", example = "50")
        val totalProcessed: Int,
    )

    @Schema(name = "GetPolygonLocationResponse")
    data class GetPolygonLocation(
        @field:Schema(description = "포토부스 위치 목록")
        val items: List<PhotoBoothLocationInfo>,
    ) {
        data class PhotoBoothLocationInfo(
            @field:Schema(description = "포토부스 ID", example = "1")
            val id: Long,

            @field:Schema(description = "브랜드 이름", example = "인생네컷")
            val brandName: String,

            @field:Schema(description = "포토부스 이름", example = "인생네컷 강남역점")
            val branchName: String,

            @field:Schema(description = "주소", example = "서울 강남구 강남대로 지하 396")
            val address: String,

            @field:Schema(description = "경도", example = "127.027456")
            val longitude: Double,

            @field:Schema(description = "위도", example = "37.497946")
            val latitude: Double,

            @field:Schema(description = "즐겨찾기 여부", example = "true")
            val favorite: Boolean,
        )
    }

    @Schema(name = "GetFavoriteMapResponse")
    data class GetFavoriteMap(
        @field:Schema(description = "즐겨찾기한 포토부스 위치 목록 (즐겨찾기한 순서)")
        val items: List<PhotoBoothLocationInfo>,
    ) {
        data class PhotoBoothLocationInfo(
            @field:Schema(description = "포토부스 ID", example = "1")
            val id: Long,

            @field:Schema(description = "브랜드 이름", example = "인생네컷")
            val brandName: String,

            @field:Schema(description = "포토부스 이름", example = "인생네컷 강남역점")
            val branchName: String,

            @field:Schema(description = "주소", example = "서울 강남구 강남대로 지하 396")
            val address: String,

            @field:Schema(description = "경도", example = "127.027456")
            val longitude: Double,

            @field:Schema(description = "위도", example = "37.497946")
            val latitude: Double,
        )
    }

    @Schema(name = "GetPointLocationResponse")
    data class GetPointLocation(
        @field:Schema(description = "포토부스 위치 목록 (거리순 정렬)")
        val items: List<PhotoBoothLocationWithDistanceInfo>,
    ) {
        data class PhotoBoothLocationWithDistanceInfo(
            @field:Schema(description = "포토부스 ID", example = "1")
            val id: Long,

            @field:Schema(description = "브랜드 이름", example = "인생네컷")
            val brandName: String,

            @field:Schema(description = "포토부스 이름", example = "인생네컷 강남역점")
            val branchName: String,

            @field:Schema(description = "주소", example = "서울 강남구 강남대로 지하 396")
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
