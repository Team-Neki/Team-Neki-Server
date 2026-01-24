package com.yapp2app.map.api.dto

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotEmpty

/**
 * fileName       : MapRequest
 * author         : darren
 * date           : 2026. 01. 13.
 * description    : Map 관련 요청 DTO
 */
data class CollectPhotoBoothRequest(
    @field:Schema(description = "검색 키워드", example = "포토이즘박스")
    @field:NotBlank
    val keyword: String,

    @field:Schema(description = "브랜드 코드", example = "PHOTOISM")
    @field:NotBlank
    val brandCode: String,
)

@Schema(
    description = "다각형 영역 내 포토부스 조회 요청",
    example = """
        {
            "coordinates": [
                {"longitude": 127.019128, "latitude": 37.502456},
                {"longitude": 127.035359, "latitude": 37.502853},
                {"longitude": 127.035663, "latitude": 37.494395},
                {"longitude": 127.023675, "latitude": 37.494257},
                {"longitude": 127.019128, "latitude": 37.502456}
            ],
            "brandIds": []
        }
    """,
)
data class GetPolygonLocationRequest(
    @field:Schema(description = "다각형을 구성하는 좌표 리스트. 첫 좌표와 마지막 좌표가 동일해야 합니다.")
    @field:NotEmpty
    val coordinates: List<Coordinate>,

    @field:Schema(description = "브랜드 ID 리스트 (nullable [] 이면 모든 브랜드)", example = "[1, 2, 3]")
    val brandIds: List<Long>? = null,
) {
    data class Coordinate(
        @field:Schema(description = "경도", example = "127.019128")
        val longitude: Double,

        @field:Schema(description = "위도", example = "37.502456")
        val latitude: Double,
    )
}

data class GetPointLocationRequest(
    @field:Schema(description = "기준점 경도", example = "127.053472")
    val longitude: Double,

    @field:Schema(description = "기준점 위도", example = "37.545348")
    val latitude: Double,

    @field:Schema(description = "검색 반경 (미터 단위)", example = "1000", defaultValue = "1000")
    @field:Min(100)
    @field:Max(50000)
    val radiusInMeters: Int = 1000,

    @field:Schema(description = "브랜드 ID 리스트 (nullable [] 이면 모든 브랜드) ", example = "[1, 2, 3]")
    val brandIds: List<Long>? = null,
)
