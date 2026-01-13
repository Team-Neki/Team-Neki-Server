package com.yapp2app.map.api.dto

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank

/**
 * fileName       : PhotoBoothRequest
 * author         : darren
 * date           : 2026. 01. 13.
 * description    : 포토부스 관련 요청 DTO
 */
data class CollectPhotoBoothRequest(
    @field:Schema(description = "검색 키워드", example = "포토이즘박스")
    @field:NotBlank
    val keyword: String,

    @field:Schema(description = "브랜드 코드", example = "PHOTOISM")
    @field:NotBlank
    val brandCode: String,
)
