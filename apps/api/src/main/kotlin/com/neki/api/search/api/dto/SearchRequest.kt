package com.neki.api.search.api.dto

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.Valid
import jakarta.validation.constraints.NotNull

/**
 * fileName       : SearchRequest
 * author         : koo
 * date           : 2026. 9. 15. 오후 5:31
 * description    : Search 관련 요청 DTO
 */
object SearchRequest {

    @Schema(
        name = "SearchPhotoBoothsRequest",
        description = "검색어에 맞는 부스 목록 요청. 검색어는 쿼리 파라미터 keyword 로 보냅니다.",
        example = """
            {
                "filterGroup": {
                    "brandFilter": { "brands": [] },
                    "sortFilter": { "type": "DEFAULT", "order": "NONE" }
                },
                "userLocation": { "latitude": 37.4979, "longitude": 127.0276 }
            }
        """,
    )
    data class GetPhotoBooths(
        @field:Schema(description = "필터 그룹. 필수이며 필터를 안 걸려면 {} 를 보냅니다")
        @field:Valid
        val filterGroup: FilterGroup,

        @field:Schema(description = "사용자 현재 위치. 실제 구현은 없으면 distance 가 null 이고 브랜드·지점명 순 정렬. 현재 mock 은 무시합니다")
        @field:Valid
        val userLocation: UserLocation? = null,
    )

    @Schema(
        name = "SearchFilterRequest",
        description = "부스 목록에서 쓸 수 있는 필터 요청. 검색어는 쿼리 파라미터 keyword 로 보냅니다.",
        example = """
            {
                "filterGroup": {
                    "brandFilter": { "brands": [] }
                }
            }
        """,
    )
    data class GetFilter(
        @field:Schema(description = "필터 그룹. 필수이며 필터를 안 걸려면 {} 를 보냅니다")
        @field:Valid
        val filterGroup: FilterGroup,
    )

    @Schema(
        name = "SearchFilterGroupRequest",
        description = "부스 목록·필터 공통 필터 그룹. 필터가 늘면 여기에 그룹이 추가됩니다.",
    )
    data class FilterGroup(
        @field:Schema(description = "브랜드 필터. 없으면 모든 브랜드")
        @field:Valid
        val brandFilter: BrandFilter? = null,

        @field:Schema(
            description = "정렬 필터. 현재 mock 은 무시합니다",
        )
        @field:Valid
        val sortFilter: SortFilter? = null,
    ) {
        @Schema(name = "SearchBrandFilterRequest")
        data class BrandFilter(
            @field:Schema(
                description = "브랜드 목록. null 또는 [] 이면 모든 브랜드. 현재 mock 은 무시합니다",
                example = """[{"brandId": 1}, {"brandId": 2}]""",
            )
            val brands: List<Brand>? = null,
        ) {
            @Schema(name = "SearchBrandRequest")
            data class Brand(
                @field:Schema(description = "브랜드 ID", example = "1")
                val brandId: Long,
            )
        }

        @Schema(name = "SearchSortFilterRequest")
        data class SortFilter(
            @field:Schema(description = "정렬 기준", example = "DEFAULT")
            val type: SortType = SortType.DEFAULT,

            @field:Schema(description = "정렬 방향", example = "NONE")
            val order: Order? = null,
        ) {
            enum class SortType {
                DEFAULT,
            }

            enum class Order {
                ASC,
                DESC,
            }
        }
    }

    @Schema(name = "SearchUserLocationRequest")
    data class UserLocation(
        @field:Schema(description = "위도", example = "37.4979")
        @field:NotNull(message = "userLocation.latitude는 필수값입니다.")
        val latitude: Double?,

        @field:Schema(description = "경도", example = "127.0276")
        @field:NotNull(message = "userLocation.longitude는 필수값입니다.")
        val longitude: Double?,
    )
}
