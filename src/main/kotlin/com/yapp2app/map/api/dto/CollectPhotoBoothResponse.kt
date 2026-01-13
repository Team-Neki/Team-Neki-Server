package com.yapp2app.map.api.dto

import io.swagger.v3.oas.annotations.media.Schema

/**
 * fileName       : PhotoBoothResponse
 * author         : darren
 * date           : 2026. 01. 13.
 * description    : 포토부스 관련 응답 DTO
 */
data class CollectPhotoBoothResponse(
    @field:Schema(description = "수집된 포토부스 수", example = "45")
    val collectedCount: Int,

    @field:Schema(description = "중복으로 스킵된 수", example = "5")
    val duplicatedCount: Int,

    @field:Schema(description = "총 처리된 수", example = "50")
    val totalProcessed: Int,
)
