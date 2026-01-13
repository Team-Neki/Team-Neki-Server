package com.yapp2app.map.api.controller

import com.yapp2app.common.api.document.RequiresSecurity
import com.yapp2app.common.api.dto.BaseResponse
import com.yapp2app.map.api.dto.CollectPhotoBoothRequest
import com.yapp2app.map.api.dto.CollectPhotoBoothResponse
import com.yapp2app.map.application.usecase.CollectPhotoBoothUseCase
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * fileName       : PhotoBoothController
 * author         : darren
 * date           : 2026. 01. 13.
 * description    : 포토부스 위치 정보 API
 */
@RequiresSecurity
@Tag(name = "photo-booth", description = "포토부스 위치 API")
@RestController
@RequestMapping("/api/photo-booths")
class PhotoBoothController(private val collectPhotoBoothUseCase: CollectPhotoBoothUseCase) {

    @Operation(
        summary = "포토부스 위치 수집 API",
        description = "Kakao Local API를 통해 포토부스 위치 정보를 수집하여 DB에 저장합니다. " +
            "첫 페이지 조회 후 meta.pageable_count를 기반으로 자동으로 모든 페이지를 순회합니다.",
    )
    @PostMapping("/collect")
    fun collectPhotoBooths(
        @Valid @RequestBody request: CollectPhotoBoothRequest,
    ): BaseResponse<CollectPhotoBoothResponse> {
        val result = collectPhotoBoothUseCase.execute(
            keyword = request.keyword,
            brandCode = request.brandCode,
        )

        val response = CollectPhotoBoothResponse(
            collectedCount = result.collectedCount,
            duplicatedCount = result.duplicatedCount,
            totalProcessed = result.totalProcessed,
        )

        return BaseResponse(data = response)
    }
}
