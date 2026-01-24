package com.yapp2app.map.api.dto

import io.swagger.v3.oas.annotations.media.Schema

/**
 * fileName       : MapResponse
 * author         : darren
 * date           : 2026. 01. 13.
 * description    : Map 관련 응답 DTO
 */
data class GetBrandResponse(
    @field:Schema(description = "브랜드 ID", example = "1")
    val id: Long,

    @field:Schema(description = "브랜드 이름", example = "포토이즘")
    val name: String,

    @field:Schema(description = "브랜드 코드", example = "PHOTOISM")
    val code: String,

    @field:Schema(description = "브랜드 이미지", example = "https://domain/file/image/xxx.png")
    val imageUrl: String?,
)

data class CollectPhotoBoothResponse(
    @field:Schema(description = "수집된 포토부스 수", example = "45")
    val collectedCount: Int,

    @field:Schema(description = "중복으로 스킵된 수", example = "5")
    val duplicatedCount: Int,

    @field:Schema(description = "총 처리된 수", example = "50")
    val totalProcessed: Int,
)

data class GetPolygonLocationResponse(
    @field:Schema(description = "포토부스 위치 목록")
    val items: List<PhotoBoothLocationInfo>,
) {
    data class PhotoBoothLocationInfo(
        @field:Schema(description = "포토부스 ID", example = "1")
        val id: Long,

        @field:Schema(description = "브랜드 ID", example = "1")
        val brandId: Long,

        @field:Schema(description = "포토부스 이름", example = "인생네컷 강남역점")
        val name: String,

        @field:Schema(description = "주소", example = "서울 강남구 강남대로 지하 396")
        val address: String,

        @field:Schema(description = "경도", example = "127.027456")
        val longitude: Double,

        @field:Schema(description = "위도", example = "37.497946")
        val latitude: Double,
    )
}

data class GetPointLocationResponse(
    @field:Schema(description = "포토부스 위치 목록 (거리순 정렬)")
    val items: List<PhotoBoothLocationWithDistanceInfo>,
) {
    data class PhotoBoothLocationWithDistanceInfo(
        @field:Schema(description = "포토부스 ID", example = "1")
        val id: Long,

        @field:Schema(description = "브랜드 ID", example = "1")
        val brandId: Long,

        @field:Schema(description = "포토부스 이름", example = "인생네컷 강남역점")
        val name: String,

        @field:Schema(description = "주소", example = "서울 강남구 강남대로 지하 396")
        val address: String,

        @field:Schema(description = "경도", example = "127.027456")
        val longitude: Double,

        @field:Schema(description = "위도", example = "37.497946")
        val latitude: Double,

        @field:Schema(description = "기준점으로부터의 거리 (미터)", example = "200")
        val distance: Int,
    )
}
