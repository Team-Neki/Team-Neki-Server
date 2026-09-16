package com.neki.api.search.api.dto

import io.swagger.v3.oas.annotations.media.Schema

/**
 * fileName       : SearchResponse
 * author         : koo
 * date           : 2026. 9. 15. 오후 5:36
 * description    : Search 관련 응답 DTO
 */
object SearchResponse {
    @Schema(name = "SearchCompletionResponse")
    data class Completion(
        @field:Schema(description = "검색 결과. 지역·역·부스 세 탭이 같은 모양이다")
        val items: List<Item>,

        @field:Schema(description = "다음 페이지 존재 여부", example = "false")
        val hasNext: Boolean,

        @field:Schema(description = "검색어에 걸린 전체 건수. 탭 건수 배지용", example = "3")
        val totalCount: Long,
    ) {
        @Schema(name = "SearchCompletionInfo")
        data class Item(
            @field:Schema(
                description = "화면에 그대로 보여 주고, 고르면 부스 목록 요청에 넘기는 값",
                example = "서울특별시 강남구",
            )
            val keyword: String,
        )
    }

    @Schema(name = "SearchPhotoBoothsResponse")
    data class GetPhotoBooths(
        @field:Schema(description = "부스 목록. 페이징 없이 전체를 내려준다")
        val items: List<Item>,
    ) {
        @Schema(name = "SearchPhotoBoothInfo")
        data class Item(
            @field:Schema(description = "부스 ID", example = "2560")
            val id: Long,

            @field:Schema(description = "브랜드 이름", example = "포토이즘")
            val brandName: String,

            @field:Schema(description = "브랜드 코드", example = "PHOTOISM")
            val brandCode: String,

            @field:Schema(description = "지점 이름", example = "강남1호점")
            val branchName: String,

            @field:Schema(description = "주소", example = "서울 강남구 강남대로102길 16")
            val address: String,

            @field:Schema(description = "위도 (소수점 7자리)", example = "37.5021077")
            val latitude: Double,

            @field:Schema(description = "경도 (소수점 7자리)", example = "127.0271830")
            val longitude: Double,

            @field:Schema(description = "사용자 위치로부터의 거리(m). 위치를 안 주면 null", example = "468", nullable = true)
            val distance: Int?,

            @field:Schema(description = "즐겨찾기 여부", example = "true")
            val favorite: Boolean,
        )
    }

    @Schema(name = "SearchFilterResponse")
    data class GetFilter(
        @field:Schema(description = "목록에 실제로 있는 브랜드만. 이미지는 브랜드 전체 조회(GET /api/photo-booths/brand)의 값을 id 로 매칭")
        val brandFilter: List<BrandFilter>,
    ) {
        @Schema(name = "SearchBrandFilterInfo")
        data class BrandFilter(
            @field:Schema(description = "브랜드 ID", example = "1")
            val id: Long,

            @field:Schema(description = "브랜드 이름", example = "포토이즘")
            val name: String,

            @field:Schema(description = "브랜드 코드", example = "PHOTOISM")
            val code: String,

            @field:Schema(description = "그 범위 안에 있는 해당 브랜드의 부스 개수", example = "9")
            val count: Int,
        )
    }
}
