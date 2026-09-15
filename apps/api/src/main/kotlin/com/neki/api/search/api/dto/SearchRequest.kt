package com.neki.api.search.api.dto

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull

/**
 * fileName       : SearchRequest
 * author         : koo
 * date           : 2026. 9. 15. 오후 5:31
 * description    : Search 관련 요청 DTO
 */
object SearchRequest {
    /**
     * 부스 목록(POST /photo-booths)과 필터(POST /filter)가 같은 body 를 쓴다.
     * 필터가 늘어도 이 클래스에 그룹만 추가하고 엔드포인트와 응답 모양은 유지한다.
     */
    @Schema(
        name = "SearchFilterGroupRequest",
        description = "고른 지역·역의 부스 목록 및 필터 요청. regionFilter 와 stationFilter 중 하나만 보냅니다.",
        example = """
            {
                "regionFilter": { "code": "1168000000" },
                "brandFilter": { "brandIds": [] },
                "userLocation": { "latitude": 37.4979, "longitude": 127.0276 }
            }
        """,
    )
    data class FilterGroup(
        @field:Schema(description = "지역 필터. stationFilter 와 둘 중 하나만 보냅니다.")
        @field:Valid
        val regionFilter: RegionFilter? = null,

        @field:Schema(description = "지하철역 필터. regionFilter 와 둘 중 하나만 보냅니다.")
        @field:Valid
        val stationFilter: StationFilter? = null,

        @field:Schema(description = "브랜드 필터. 없으면 모든 브랜드")
        @field:Valid
        val brandFilter: BrandFilter? = null,

        @field:Schema(description = "사용자 현재 위치. 없으면 distance 가 null 이고 브랜드·지점명 순 정렬. 필터 API 는 무시")
        @field:Valid
        val userLocation: UserLocation? = null,
    ) {
        data class RegionFilter(
            @field:Schema(description = "법정동코드 10자리 (지역 검색 응답의 code)", example = "1168000000")
            @field:NotBlank(message = "regionFilter.code는 필수값입니다.")
            val code: String?,
        )

        data class StationFilter(
            @field:Schema(description = "역명. 역 접미사 없음 (역 검색 응답의 name)", example = "강남")
            @field:NotBlank(message = "stationFilter.name은 필수값입니다.")
            val name: String?,

            @field:Schema(description = "노선명 (역 검색 응답의 lineName)", example = "신분당선")
            @field:NotBlank(message = "stationFilter.lineName은 필수값입니다.")
            val lineName: String?,
        )

        data class BrandFilter(
            @field:Schema(description = "브랜드 ID 리스트 (null 또는 [] 이면 모든 브랜드)", example = "[1, 2]")
            val brandIds: List<Long>? = null,
        )

        data class UserLocation(
            @field:Schema(description = "위도", example = "37.4979")
            @field:NotNull(message = "userLocation.latitude는 필수값입니다.")
            val latitude: Double?,

            @field:Schema(description = "경도", example = "127.0276")
            @field:NotNull(message = "userLocation.longitude는 필수값입니다.")
            val longitude: Double?,
        )
    }
}
