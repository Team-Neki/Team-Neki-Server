package com.neki.api.map.api.dto

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
}
