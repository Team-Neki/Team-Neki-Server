package com.neki.api.map.api.controller

import com.neki.api.common.api.document.RequiresSecurity
import com.neki.api.map.api.dto.MapConverter
import com.neki.api.map.api.dto.MapRequest
import com.neki.api.map.api.dto.MapResponse
import com.neki.api.map.application.CollectPhotoBoothLocationUseCase
import com.neki.api.map.application.GetBrandUseCase
import com.neki.api.map.application.UpdateBrandOrderUseCase
import com.neki.api.map.application.dto.MapResult
import com.neki.core.api.dto.BaseResponse
import com.neki.domain.map.dto.MapCommand
import com.neki.domain.map.dto.MapQuery
import io.swagger.v3.oas.annotations.Hidden
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * fileName       : MapController
 * author         : darren
 * date           : 2026. 01. 13.
 * description    : 포토부스 브랜드 API. 부스 조회·즐겨찾기는 V2(/api/v2/photo-booths) 를 쓴다
 */
@RequiresSecurity
@Tag(name = "photo-booth", description = "포토부스 위치 API")
@RestController
@RequestMapping("/api/photo-booths")
class MapController(
    private val getBrandUseCase: GetBrandUseCase,
    private val updateBrandOrderUseCase: UpdateBrandOrderUseCase,
    private val collectPhotoBoothLocationUseCase: CollectPhotoBoothLocationUseCase,
    private val requestConverter: MapConverter.RequestConverter,
    private val responseConverter: MapConverter.ResponseConverter,
) {

    @Operation(
        summary = "브랜드 종류 조회 API",
        description = """
            브랜드 종류 및 이미지를 조회합니다.
            사용자가 정렬 순서를 저장한 경우(PUT /api/photo-booths/brand/order) 저장한 순서대로,
            저장하지 않은 경우 서버 기본 순서대로 반환합니다.
            """,
    )
    @GetMapping("/brand")
    fun getBrand(@AuthenticationPrincipal(expression = "id") userId: Long): BaseResponse<List<MapResponse.GetBrand>> {
        val result: List<MapResult.GetBrand> = getBrandUseCase.execute(MapQuery.GetBrand(userId))

        val response: List<MapResponse.GetBrand> = responseConverter.toGetBrandResponse(result)

        return BaseResponse(data = response)
    }

    @Operation(
        summary = "브랜드 정렬 순서 저장 API",
        description = """
            사용자가 커스텀한 브랜드 정렬 순서를 저장합니다.
            brandIds 에 보여주고자 하는 순서대로 브랜드 ID 를 전달하면, 이후 브랜드 조회 API 가 해당 순서로 반환합니다.
            전체 순서를 덮어쓰는 방식(멱등)이며, 다시 호출하면 기존 순서는 새 순서로 대체됩니다.
            """,
    )
    @PutMapping("/brand/order")
    fun updateBrandOrder(
        @AuthenticationPrincipal(expression = "id") userId: Long,
        @Valid @RequestBody request: MapRequest.UpdateBrandOrder,
    ): BaseResponse<Any> {
        val command: MapCommand.UpdateBrandOrder = requestConverter.toUpdateBrandOrderCommand(userId, request)

        updateBrandOrderUseCase.execute(command)

        return BaseResponse()
    }

    @Operation(
        summary = "포토부스 위치 수집 API",
        description = """
            Kakao Local API를 통해 포토부스 위치 정보를 수집하여 DB에 저장합니다.
            첫 페이지 조회 후 meta.pageable_count를 기반으로 자동으로 모든 페이지를 순회합니다.
            """,
        deprecated = true,
    )
    @Deprecated("Prefect 배치로 이관 예정. 이관 완료 후 제거한다")
    @Hidden
    @PostMapping("/collect")
    fun collectPhotoBooths(
        @Valid @RequestBody request: MapRequest.CollectPhotoBooth,
    ): BaseResponse<MapResponse.CollectPhotoBooth> {
        val command: MapCommand.CollectPhotoBooth = requestConverter.toCollectPhotoBoothCommand(request)

        val result: MapResult.CollectPhotoBooth = collectPhotoBoothLocationUseCase.execute(command)

        val response: MapResponse.CollectPhotoBooth = responseConverter.toCollectPhotoBoothResponse(result)

        return BaseResponse(data = response)
    }
}
