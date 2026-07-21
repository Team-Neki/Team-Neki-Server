package com.neki.map.api.controller

import com.neki.common.api.document.RequiresSecurity
import com.neki.common.api.dto.BaseResponse
import com.neki.map.api.dto.CollectPhotoBoothRequest
import com.neki.map.api.dto.CollectPhotoBoothResponse
import com.neki.map.api.dto.GetBrandResponse
import com.neki.map.api.dto.GetPointLocationRequest
import com.neki.map.api.dto.GetPointLocationResponse
import com.neki.map.api.dto.GetPolygonLocationRequest
import com.neki.map.api.dto.GetPolygonLocationResponse
import com.neki.map.api.dto.MapConverter
import com.neki.map.api.dto.UpdateBrandOrderRequest
import com.neki.map.application.dto.MapCommand
import com.neki.map.application.dto.MapQuery
import com.neki.map.application.dto.MapResult
import com.neki.map.application.usecase.CollectPhotoBoothLocationUseCase
import com.neki.map.application.usecase.GetBrandUseCase
import com.neki.map.application.usecase.GetPhotoBoothLocationUseCase
import com.neki.map.application.usecase.UpdateBrandOrderUseCase
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
 * description    : 포토부스 위치 정보 API
 */
@RequiresSecurity
@Tag(name = "photo-booth", description = "포토부스 위치 API")
@RestController
@RequestMapping("/api/photo-booths")
class MapController(
    private val getBrandUseCase: GetBrandUseCase,
    private val updateBrandOrderUseCase: UpdateBrandOrderUseCase,
    private val collectPhotoBoothLocationUseCase: CollectPhotoBoothLocationUseCase,
    private val getPhotoBoothLocationUseCase: GetPhotoBoothLocationUseCase,
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
    fun getBrand(@AuthenticationPrincipal(expression = "id") userId: Long): BaseResponse<List<GetBrandResponse>> {
        val result: List<MapResult.GetBrand> = getBrandUseCase.execute(userId)

        val response: List<GetBrandResponse> = responseConverter.toGetBrandResponse(result)

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
        @Valid @RequestBody request: UpdateBrandOrderRequest,
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
    )
    @Hidden
    @PostMapping("/collect")
    fun collectPhotoBooths(
        @Valid @RequestBody request: CollectPhotoBoothRequest,
    ): BaseResponse<CollectPhotoBoothResponse> {
        val command: MapCommand.CollectPhotoBooth = requestConverter.toCollectPhotoBoothCommand(request)

        val result: MapResult.CollectPhotoBooth = collectPhotoBoothLocationUseCase.execute(command)

        val response: CollectPhotoBoothResponse = responseConverter.toCollectPhotoBoothResponse(result)

        return BaseResponse(data = response)
    }

    @Operation(
        summary = "다각형 영역 내 포토부스 조회 API",
        description = """
            다각형 영역 내의 포토부스 위치 정보를 조회합니다.
            coordinates의 첫 좌표와 마지막 좌표는 동일해야 합니다 (다각형을 닫기 위함).

            example에 있는 위치는 강남역 기준
            """,
    )
    @PostMapping("/polygon")
    fun getPhotoBoothsByPolygon(
        @AuthenticationPrincipal(expression = "id") userId: Long,
        @Valid @RequestBody request: GetPolygonLocationRequest,
    ): BaseResponse<GetPolygonLocationResponse> {
        val query: MapQuery.GetPolygonLocation = requestConverter.toGetPolygonLocationQuery(userId, request)

        val result: MapResult.GetPolygonLocation = getPhotoBoothLocationUseCase.execute(query)

        val response: GetPolygonLocationResponse = responseConverter.toGetPolygonLocationResponse(result)

        return BaseResponse(data = response)
    }

    @Operation(
        summary = "특정 좌표 기준 반경 내 포토부스 조회 API",
        description = """
            특정 좌표를 기준으로 반경(radiusInMeters) 내의 포토부스를 거리순으로 조회합니다.
            거리는 미터 단위 정수로 반환됩니다.
            radiusInMeters = 1000 (1KM)
            example에 있는 위치는 강남역 기준
            """,
    )
    @PostMapping("/point")
    fun getPhotoBoothsByPoint(
        @AuthenticationPrincipal(expression = "id") userId: Long,
        @Valid @RequestBody request: GetPointLocationRequest,
    ): BaseResponse<GetPointLocationResponse> {
        val query: MapQuery.GetPointLocation = requestConverter.toGetPointLocationQuery(userId, request)

        val result: MapResult.GetPointLocation = getPhotoBoothLocationUseCase.execute(query)

        val response: GetPointLocationResponse = responseConverter.toGetPointLocationResponse(result)

        return BaseResponse(data = response)
    }
}
