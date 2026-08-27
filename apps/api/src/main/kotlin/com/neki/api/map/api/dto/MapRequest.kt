package com.neki.api.map.api.dto

import com.neki.api.map.api.validation.ClosedPolygon
import com.neki.api.map.api.validation.MAX_POLYGON_POINTS
import com.neki.api.map.api.validation.MAX_POLYGON_POINTS_MESSAGE
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.Valid
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size

/**
 * fileName       : MapRequest
 * author         : koo
 * date           : 2026. 7. 21.
 * description    : Map 관련 요청 DTO
 */
object MapRequest {
    @Schema(name = "CollectPhotoBoothRequest")
    data class CollectPhotoBooth(
        @field:Schema(description = "검색 키워드", example = "포토이즘박스")
        @field:NotBlank
        val keyword: String?,

        @field:Schema(description = "브랜드 코드", example = "PHOTOISM")
        @field:NotBlank
        val brandCode: String?,
    )

    @Schema(
        name = "GetPolygonLocationRequest",
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
    data class GetPolygonLocation(
        @field:Schema(
            description = "다각형을 구성하는 좌표 리스트. 4개 이상 ${MAX_POLYGON_POINTS}개 이하이며 첫 좌표와 마지막 좌표가 동일해야 합니다.",
        )
        @field:NotEmpty
        @field:Size(max = MAX_POLYGON_POINTS, message = MAX_POLYGON_POINTS_MESSAGE)
        @field:Valid
        @field:ClosedPolygon
        val coordinates: List<Coordinate>,

        @field:Schema(description = "브랜드 ID 리스트 (nullable [] 이면 모든 브랜드)", example = "[1, 2, 3]")
        val brandIds: List<Long>? = null,
    ) {
        data class Coordinate(
            @field:Schema(description = "경도", example = "127.019128")
            @field:NotNull(message = "longitude은 필수값입니다.")
            var longitude: Double?,

            @field:Schema(description = "위도", example = "37.502456")
            @field:NotNull(message = "latitude은 필수값입니다.")
            var latitude: Double?,
        )
    }

    @Schema(
        name = "FilterGroupRequest",
        description = "지도 필터 조회 요청. 필터가 추가되면 이 요청에 필터 필드가 늘어난다.",
        example = """
            {
                "polygonFilter": {
                    "coordinates": [
                        {"longitude": 127.019128, "latitude": 37.502456},
                        {"longitude": 127.035359, "latitude": 37.502853},
                        {"longitude": 127.035663, "latitude": 37.494395},
                        {"longitude": 127.023675, "latitude": 37.494257},
                        {"longitude": 127.019128, "latitude": 37.502456}
                    ]
                },
                "brandFilter": {
                    "brandIds": []
                }
            }
        """,
    )
    data class FilterGroup(
        @field:Schema(description = "조회할 다각형 영역")
        @field:NotNull(message = "polygonFilter는 필수값입니다.")
        @field:Valid
        val polygonFilter: PolygonFilter?,

        @field:Schema(description = "브랜드 필터 (생략하면 모든 브랜드)")
        @field:Valid
        val brandFilter: BrandFilter? = null,
    ) {
        @Schema(name = "PolygonFilterRequest", description = "다각형 영역 필터")
        data class PolygonFilter(
            @field:Schema(
                description = "다각형을 구성하는 좌표 리스트. " +
                    "4개 이상 ${MAX_POLYGON_POINTS}개 이하이며 첫 좌표와 마지막 좌표가 동일해야 합니다.",
            )
            @field:NotEmpty
            @field:Size(max = MAX_POLYGON_POINTS, message = MAX_POLYGON_POINTS_MESSAGE)
            @field:Valid
            @field:ClosedPolygon
            val coordinates: List<GetPolygonLocation.Coordinate>,
        )

        @Schema(name = "BrandFilterRequest", description = "브랜드 필터")
        data class BrandFilter(
            @field:Schema(description = "브랜드 ID 리스트 (null 이거나 [] 이면 모든 브랜드)", example = "[1, 2, 3]")
            val brandIds: List<Long>? = null,
        )
    }

    @Schema(name = "GetPointLocationRequest")
    data class GetPointLocation(
        @field:Schema(description = "기준점 경도", example = "127.0276")
        val longitude: Double? = null,

        @field:Schema(description = "기준점 위도", example = "37.4979")
        val latitude: Double? = null,

        @field:Schema(description = "검색 반경 (미터 단위)", example = "1000", defaultValue = "1000")
        @field:Min(100)
        @field:Max(50000)
        val radiusInMeters: Int = 1000,

        @field:Schema(description = "브랜드 ID 리스트 (nullable [] 이면 모든 브랜드) ", example = "[1, 2, 3]")
        val brandIds: List<Long>? = null,
    )

    @Schema(name = "UpdateMapFavoriteRequest")
    data class UpdateMapFavorite(
        @field:Schema(description = "변경하고자 하는 즐겨찾기 상태", example = "true")
        @field:NotNull(message = "favorite은 필수값입니다.")
        val favorite: Boolean?,
    )

    @Schema(name = "UpdateBrandOrderRequest")
    data class UpdateBrandOrder(
        @field:Schema(
            description = "정렬할 브랜드 ID 리스트. 보여주고자 하는 순서대로 전달합니다.",
            example = "[3, 1, 2]",
        )
        @field:NotEmpty(message = "brandIds는 필수값입니다.")
        val brandIds: List<Long>,
    )
}
